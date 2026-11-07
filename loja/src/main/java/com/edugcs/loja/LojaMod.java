package com.edugcs.loja;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.block.WallSignBlock;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.text.MutableText;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lojas com baú e placa, só no servidor.
 *  - Criar: baú/barril + placa com "[loja]" na 1ª linha. Clique na placa segurando o item que vai vender
 *    (mão principal) e o item que quer receber (outra mão). Quantidades: linhas 2 e 3 da placa, ou o
 *    tamanho das pilhas que estiver segurando.
 *  - Comprar: clique na placa (agachado = até 10 trocas seguidas).
 *  - Só o dono (ou OP) abre o baú e quebra a placa/baú. Quebrar a placa desfaz a loja.
 *  - Baú privado: placa com "[privado]" na 1ª linha (linhas 2 a 4: amigos que também podem abrir).
 *    Clique na placa para trancar. Quebrar a placa destranca.
 */
public class LojaMod implements ModInitializer {

    private static final Armazem ARMAZEM = new Armazem();
    private static Path arquivo;
    private static final int TROCAS_AGACHADO = 10;

    private static final Troca.Ops<ItemStack> OPS = new Troca.Ops<ItemStack>() {
        public boolean vazio(ItemStack s) { return s.isEmpty(); }
        public int qtd(ItemStack s) { return s.getCount(); }
        public int max(ItemStack s) { return s.getMaxCount(); }
        public boolean igual(ItemStack m, ItemStack s) { return ItemStack.canCombine(m, s); } // item + NBT
        public ItemStack comQtd(ItemStack m, int q) { return q <= 0 ? ItemStack.EMPTY : m.copyWithCount(q); }
    };

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register(LojaMod::aoUsar);
        PlayerBlockBreakEvents.BEFORE.register(LojaMod::antesDeQuebrar);
    }

    // =====================================================================  eventos

    private static ActionResult aoUsar(PlayerEntity jogador, World mundo, Hand mao, BlockHitResult hit) {
        if (mundo.isClient) return ActionResult.PASS;
        try {
            carregar(jogador);
            BlockPos pos = hit.getBlockPos();
            String dim = dim(mundo);
            boolean principal = mao == Hand.MAIN_HAND;

            // ---- placa de loja já criada
            Armazem.Registro loja = ARMAZEM.porPlaca(dim, x(pos), y(pos), z(pos));
            if (loja != null) {
                if (!principal) return ActionResult.SUCCESS; // já tratado na mão principal
                if (loja.privado()) infoPrivado(jogador, loja);
                else if (dono(jogador, loja)) infoDono(jogador, mundo, loja);
                else comprar(jogador, mundo, loja);
                return ActionResult.SUCCESS;
            }

            // ---- placa nova com [loja]
            BlockEntity be = mundo.getBlockEntity(pos);
            if (be instanceof SignBlockEntity) {
                SignBlockEntity placa = (SignBlockEntity) be;
                String l0 = linha(placa, 0);
                if (l0.equalsIgnoreCase("[loja]")) {
                    if (principal) criar(jogador, mundo, pos, placa);
                    return ActionResult.SUCCESS;
                }
                if (ehPlacaPrivada(l0)) {
                    if (principal) criarPrivado(jogador, mundo, pos, placa);
                    return ActionResult.SUCCESS;
                }
            }

            // ---- baú de loja: só o dono abre
            Armazem.Registro doBau = protegido(mundo, dim, pos);
            if (doBau != null && !podeAbrir(jogador, mundo, dim, pos)) {
                if (principal) {
                    if (doBau.privado()) msg(jogador, "§cEsse baú é privado de " + doBau.nomeDono + ".");
                    else msg(jogador, "§cEsse baú é a loja de " + doBau.nomeDono + ". Clique na placa para comprar.");
                }
                return ActionResult.FAIL;
            }

            // ---- não deixa colocar funil embaixo do baú de uma loja
            ItemStack naMao = principal ? jogador.getMainHandStack() : jogador.getOffHandStack();
            if (!naMao.isEmpty()) {
                String chave = naMao.getItem().getTranslationKey();
                if (chave.equals("block.minecraft.hopper") || chave.equals("item.minecraft.hopper_minecart")) {
                    BlockPos lugar = pos.offset(hit.getSide());
                    for (BlockPos p : new BlockPos[]{pos, lugar}) {
                        Armazem.Registro acima = protegido(mundo, dim, p.up());
                        if (acima != null && !podeMexer(jogador, acima)) {
                            msg(jogador, "§cNão dá para colocar funil embaixo da loja de " + acima.nomeDono + ".");
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[Loja] Erro ao usar bloco: " + t);
        }
        return ActionResult.PASS;
    }

    private static boolean antesDeQuebrar(World mundo, PlayerEntity jogador, BlockPos pos, BlockState estado, BlockEntity be) {
        if (mundo.isClient) return true;
        try {
            carregar(jogador);
            String dim = dim(mundo);
            Armazem.Registro placa = ARMAZEM.porPlaca(dim, x(pos), y(pos), z(pos));
            if (placa != null) {
                if (!podeMexer(jogador, placa)) {
                    msg(jogador, placa.privado() ? "§cEssa placa tranca o baú de " + placa.nomeDono + "."
                                                 : "§cEssa placa é da loja de " + placa.nomeDono + ".");
                    return false;
                }
                ARMAZEM.lojas.remove(placa);
                salvar();
                msg(jogador, placa.privado() ? "§eBaú destrancado." : "§eLoja desfeita.");
                return true;
            }
            Armazem.Registro bau = protegido(mundo, dim, pos);
            if (bau != null) {
                if (!podeMexer(jogador, bau)) {
                    msg(jogador, bau.privado() ? "§cEsse baú é privado de " + bau.nomeDono + "."
                                               : "§cEsse baú é a loja de " + bau.nomeDono + ".");
                    return false;
                }
                List<Armazem.Registro> doBau = ARMAZEM.todasDoBau(dim, x(pos), y(pos), z(pos));
                if (!doBau.isEmpty()) {
                    ARMAZEM.lojas.removeAll(doBau);
                    salvar();
                    msg(jogador, "§eO baú foi quebrado: loja/tranca desfeita.");
                }
            }
        } catch (Throwable t) {
            System.err.println("[Loja] Erro ao quebrar bloco: " + t);
        }
        return true;
    }

    // =====================================================================  criar

    private static void criar(PlayerEntity jogador, World mundo, BlockPos posPlaca, SignBlockEntity placa) throws Exception {
        ItemStack vende = jogador.getMainHandStack(), recebe = jogador.getOffHandStack();
        if (vende.isEmpty() || recebe.isEmpty()) {
            msg(jogador, "§eComo criar a loja: segure o item que vai VENDER na mão principal e o item que quer RECEBER na outra mão (tecla F troca de mão), e clique na placa.");
            msg(jogador, "§7As quantidades são as das pilhas que você está segurando, ou escreva na placa: linha 2 = quantos vende, linha 3 = quantos recebe.");
            return;
        }
        int vendeQtd = Armazem.numeroDaLinha(linha(placa, 1));
        int recebeQtd = Armazem.numeroDaLinha(linha(placa, 2));
        if (vendeQtd <= 0) vendeQtd = vende.getCount();
        if (recebeQtd <= 0) recebeQtd = recebe.getCount();
        if (vendeQtd > 2304 || recebeQtd > 2304) {
            msg(jogador, "§cQuantidade grande demais (máximo 2304, um inventário inteiro).");
            return;
        }

        BlockPos posBau = bauDaPlaca(mundo, posPlaca);
        if (posBau == null) {
            msg(jogador, "§cColoque a placa em um baú ou barril (na frente dele ou em cima).");
            return;
        }
        String dim = dim(mundo);
        Armazem.Registro outro = protegido(mundo, dim, posBau);
        if (outro != null && !outro.dono.equals(id(jogador))) {
            msg(jogador, outro.privado() ? "§cEsse baú é privado de " + outro.nomeDono + "."
                                         : "§cEsse baú já é a loja de " + outro.nomeDono + ".");
            return;
        }
        for (BlockPos p : partesDoBau(mundo, posBau)) {
            if (mundo.getBlockEntity(p.down()) instanceof HopperBlockEntity) { // tem funil embaixo
                msg(jogador, "§cTire o funil de baixo do baú antes de criar a loja.");
                return;
            }
        }

        Armazem.Registro r = new Armazem.Registro();
        r.dim = dim;
        r.px = x(posPlaca); r.py = y(posPlaca); r.pz = z(posPlaca);
        r.bx = x(posBau); r.by = y(posBau); r.bz = z(posBau);
        r.dono = id(jogador);
        r.nomeDono = nome(jogador);
        r.vende = vende.copyWithCount(1);
        r.recebe = recebe.copyWithCount(1);
        r.vendeNbt = ((ItemStack) r.vende).writeNbt(new NbtCompound()).asString();
        r.recebeNbt = ((ItemStack) r.recebe).writeNbt(new NbtCompound()).asString();
        r.vendeQtd = vendeQtd;
        r.recebeQtd = recebeQtd;
        ARMAZEM.lojas.add(r);
        salvar();

        // escreve a placa e encera para ninguém editar
        SignText txt = placa.getFrontText();
        txt = txt.withMessage(0, Text.literal("§1§l[Loja]"))
                 .withMessage(1, texto("§0" + vendeQtd + "x ", (ItemStack) r.vende))
                 .withMessage(2, texto("§2por " + recebeQtd + "x ", (ItemStack) r.recebe))
                 .withMessage(3, Text.literal("§8" + r.nomeDono));
        placa.setText(txt, true);
        placa.setWaxed(true);

        MutableText m = Text.literal("§aLoja criada! Vende " + vendeQtd + "x ");
        m.append(((ItemStack) r.vende).getName());
        m.append(Text.literal("§a por " + recebeQtd + "x "));
        m.append(((ItemStack) r.recebe).getName());
        m.append(Text.literal("§a. Coloque o estoque no baú."));
        jogador.sendMessage(m, false);
    }

    // =====================================================================  baú privado

    private static boolean ehPlacaPrivada(String l0) {
        return l0.equalsIgnoreCase("[privado]") || l0.equalsIgnoreCase("[private]") || l0.equalsIgnoreCase("[trancado]");
    }

    private static void criarPrivado(PlayerEntity jogador, World mundo, BlockPos posPlaca, SignBlockEntity placa) throws Exception {
        BlockPos posBau = bauDaPlaca(mundo, posPlaca);
        if (posBau == null) {
            msg(jogador, "§cColoque a placa em um baú, barril ou fornalha (na frente dele ou em cima).");
            return;
        }
        String dim = dim(mundo);
        Armazem.Registro outro = protegido(mundo, dim, posBau);
        if (outro != null) {
            if (!outro.dono.equals(id(jogador))) {
                msg(jogador, outro.privado() ? "§cEsse baú já é privado de " + outro.nomeDono + "."
                                             : "§cEsse baú é a loja de " + outro.nomeDono + ".");
            } else {
                msg(jogador, outro.privado() ? "§eEsse baú já está trancado por você."
                                             : "§eEsse baú já é a sua loja, ele já fica trancado.");
            }
            return;
        }

        Armazem.Registro r = new Armazem.Registro();
        r.tipo = "privado";
        r.dim = dim;
        r.px = x(posPlaca); r.py = y(posPlaca); r.pz = z(posPlaca);
        r.bx = x(posBau); r.by = y(posBau); r.bz = z(posBau);
        r.dono = id(jogador);
        r.nomeDono = nome(jogador);
        for (int i = 1; i <= 3; i++) {
            String n = linha(placa, i);
            if (!n.isEmpty() && !n.equalsIgnoreCase(r.nomeDono) && !r.amigo(n)) r.amigos.add(n);
        }
        ARMAZEM.lojas.add(r);
        salvar();

        String l2 = r.amigos.size() > 0 ? r.amigos.get(0) : "";
        String l3 = r.amigos.size() > 1 ? String.join(", ", r.amigos.subList(1, r.amigos.size())) : "";
        SignText txt = placa.getFrontText();
        txt = txt.withMessage(0, Text.literal("§4§l[Privado]"))
                 .withMessage(1, Text.literal("§8" + r.nomeDono))
                 .withMessage(2, Text.literal("§2" + l2))
                 .withMessage(3, Text.literal("§2" + l3));
        placa.setText(txt, true);
        placa.setWaxed(true);

        msg(jogador, "§aBaú trancado! Só você" + (r.amigos.isEmpty() ? "" : " e " + String.join(", ", r.amigos))
                + " pode" + (r.amigos.isEmpty() ? "" : "m") + " abrir. Para destrancar, quebre a placa.");
    }

    private static void infoPrivado(PlayerEntity jogador, Armazem.Registro r) {
        if (dono(jogador, r)) {
            msg(jogador, "§eSeu baú privado." + (r.amigos.isEmpty() ? "" : " Amigos com acesso: " + String.join(", ", r.amigos) + ".")
                    + " Para destrancar ou mudar os amigos, quebre a placa e faça de novo.");
        } else {
            msg(jogador, "§7Baú privado de " + r.nomeDono + ".");
        }
    }

    /** Quem pode abrir o baú nessa posição: dono, OP, ou amigo de algum baú privado do mesmo baú. */
    private static boolean podeAbrir(PlayerEntity j, World mundo, String dim, BlockPos pos) {
        Armazem.Registro r = protegido(mundo, dim, pos);
        if (r == null || podeMexer(j, r)) return true;
        // pode haver mais de um registro no mesmo baú (ex.: placa privada + loja do mesmo dono)
        String nome = nome(j);
        for (BlockPos p : partesDoBau(mundo, pos)) {
            for (Armazem.Registro x : ARMAZEM.todasDoBau(dim, x(p), y(p), z(p))) {
                if (x.privado() && x.amigo(nome)) return true;
            }
        }
        for (BlockPos v : vizinhos(pos)) {
            if (!(mundo.getBlockEntity(v) instanceof ChestBlockEntity) || !(mundo.getBlockEntity(pos) instanceof ChestBlockEntity)) continue;
            for (Armazem.Registro x : ARMAZEM.todasDoBau(dim, x(v), y(v), z(v))) {
                if (x.privado() && x.amigo(nome)) return true;
            }
        }
        return false;
    }

    // =====================================================================  comprar

    private static void comprar(PlayerEntity jogador, World mundo, Armazem.Registro loja) {
        Inventory bau = HopperBlockEntity.getInventoryAt(mundo, new BlockPos(loja.bx, loja.by, loja.bz)); // junta baú duplo
        if (bau == null || !(mundo.getBlockEntity(new BlockPos(loja.bx, loja.by, loja.bz)) instanceof Inventory)) {
            msg(jogador, "§cO baú dessa loja sumiu. A loja foi desfeita.");
            ARMAZEM.lojas.remove(loja);
            salvarSilencioso();
            return;
        }
        PlayerInventory invJog = jogador.getInventory();
        ItemStack vende = (ItemStack) loja.vende, recebe = (ItemStack) loja.recebe;
        int vezes = jogador.isSneaking() ? TROCAS_AGACHADO : 1;
        int feitas = 0;
        Troca.Resultado ultimo = Troca.Resultado.OK;
        for (int n = 0; n < vezes; n++) {
            List<ItemStack> b = copiar(bau, null);
            int[] idxJog = indicesJogador();
            List<ItemStack> j = copiar(invJog, idxJog);
            int[] sobra = new int[1];
            ultimo = Troca.trocar(b, j, 36, vende, loja.vendeQtd, recebe, loja.recebeQtd, OPS, sobra);
            if (ultimo != Troca.Resultado.OK) break;
            gravar(bau, b, null);
            gravar(invJog, j, idxJog);
            if (sobra[0] > 0) soltar(jogador, vende, sobra[0]);
            feitas++;
        }
        if (feitas > 0) {
            bau.markDirty();
            invJog.markDirty();
            String vx = feitas > 1 ? " §7(" + feitas + " trocas)" : "";
            MutableText m = Text.literal("§aVocê pagou " + (loja.recebeQtd * feitas) + "x ");
            m.append(recebe.getName());
            m.append(Text.literal("§a e levou " + (loja.vendeQtd * feitas) + "x "));
            m.append(vende.getName());
            m.append(Text.literal("§a da loja de " + loja.nomeDono + "." + vx));
            jogador.sendMessage(m, false);
            avisarDono(jogador, loja, feitas);
        }
        if (ultimo != Troca.Resultado.OK && (feitas == 0 || vezes > 1)) {
            switch (ultimo) {
                case SEM_ESTOQUE: msg(jogador, "§c" + (feitas > 0 ? "Acabou o estoque." : "Essa loja está sem estoque.")); break;
                case BAU_CHEIO: msg(jogador, "§cO baú dessa loja está cheio."); break;
                case SEM_PAGAMENTO: {
                    MutableText m = Text.literal("§c" + (feitas > 0 ? "Acabou o pagamento. " : "") + "Você precisa de " + loja.recebeQtd + "x ");
                    m.append(recebe.getName());
                    m.append(Text.literal("§c para comprar."));
                    jogador.sendMessage(m, false);
                    break;
                }
                default: break;
            }
        }
    }

    private static void infoDono(PlayerEntity jogador, World mundo, Armazem.Registro loja) {
        Inventory bau = HopperBlockEntity.getInventoryAt(mundo, new BlockPos(loja.bx, loja.by, loja.bz));
        ItemStack vende = (ItemStack) loja.vende, recebe = (ItemStack) loja.recebe;
        MutableText m = Text.literal("§eSua loja vende " + loja.vendeQtd + "x ");
        m.append(vende.getName());
        m.append(Text.literal("§e por " + loja.recebeQtd + "x "));
        m.append(recebe.getName());
        m.append(Text.literal("§e."));
        jogador.sendMessage(m, false);
        if (bau != null) {
            List<ItemStack> b = copiar(bau, null);
            int estoque = Troca.contar(b, vende, OPS) / Math.max(1, loja.vendeQtd);
            int guardado = Troca.contar(b, recebe, OPS);
            MutableText m2 = Text.literal("§7Estoque para " + estoque + " troca" + (estoque == 1 ? "" : "s") + ". No baú: " + guardado + "x ");
            m2.append(recebe.getName());
            m2.append(Text.literal("§7. Para desfazer, quebre a placa."));
            jogador.sendMessage(m2, false);
        }
    }

    private static void avisarDono(PlayerEntity comprador, Armazem.Registro loja, int feitas) {
        try {
            MinecraftServer server = comprador.getServer();
            if (server == null) return;
            ServerPlayerEntity dono = server.getPlayerManager().getPlayer(loja.dono);
            if (dono == null) return;
            MutableText m = Text.literal("§e[Loja] " + nome(comprador) + " levou " + (loja.vendeQtd * feitas) + "x ");
            m.append(((ItemStack) loja.vende).getName());
            m.append(Text.literal("§e e deixou " + (loja.recebeQtd * feitas) + "x "));
            m.append(((ItemStack) loja.recebe).getName());
            m.append(Text.literal("§e no seu baú."));
            dono.sendMessage(m, false);
        } catch (Throwable ignorado) {
        }
    }

    // =====================================================================  apoio

    /** Slots do jogador usados na troca: 0-35 (inventário) + 40 (mão secundária). */
    private static int[] indicesJogador() {
        int[] idx = new int[37];
        for (int i = 0; i < 36; i++) idx[i] = i;
        idx[36] = 40;
        return idx;
    }

    private static List<ItemStack> copiar(Inventory inv, int[] idx) {
        List<ItemStack> l = new ArrayList<>();
        if (idx == null) for (int i = 0; i < inv.size(); i++) l.add(inv.getStack(i).copy());
        else for (int i : idx) l.add(inv.getStack(i).copy());
        return l;
    }

    private static void gravar(Inventory inv, List<ItemStack> l, int[] idx) {
        for (int i = 0; i < l.size(); i++) inv.setStack(idx == null ? i : idx[i], l.get(i));
    }

    private static void soltar(PlayerEntity jogador, ItemStack modelo, int qtd) {
        int max = Math.max(1, modelo.getMaxCount());
        while (qtd > 0) {
            int q = Math.min(max, qtd);
            jogador.dropItem(modelo.copyWithCount(q), false);
            qtd -= q;
        }
    }

    /** Onde fica o baú da placa: atrás da placa de parede, acima da placa pendurada, ou embaixo da placa de chão. */
    private static BlockPos bauDaPlaca(World mundo, BlockPos pos) {
        BlockState estado = mundo.getBlockState(pos);
        List<BlockPos> opcoes = new ArrayList<>();
        Object bloco = estado.getBlock();
        if (bloco instanceof WallSignBlock) {
            Direction frente = (Direction) estado.get(WallSignBlock.FACING);
            opcoes.add(pos.offset(frente.getOpposite()));
        } else if (bloco instanceof HangingSignBlock) {
            opcoes.add(pos.up());
        } else {
            opcoes.add(pos.down());
        }
        for (BlockPos p : opcoes) if (mundo.getBlockEntity(p) instanceof Inventory) return p;
        return null;
    }

    /** Posição do baú e da outra metade (se for baú duplo). */
    private static List<BlockPos> partesDoBau(World mundo, BlockPos bau) {
        List<BlockPos> l = new ArrayList<>();
        l.add(bau);
        if (mundo.getBlockEntity(bau) instanceof ChestBlockEntity) {
            for (BlockPos v : vizinhos(bau)) if (mundo.getBlockEntity(v) instanceof ChestBlockEntity) l.add(v);
        }
        return l;
    }

    private static BlockPos[] vizinhos(BlockPos p) {
        return new BlockPos[]{p.north(), p.south(), p.east(), p.west()};
    }

    /** Loja que protege esse bloco: o próprio baú da loja ou um baú colado nele (metade do baú duplo). */
    private static Armazem.Registro protegido(World mundo, String dim, BlockPos pos) {
        Armazem.Registro r = ARMAZEM.porBau(dim, x(pos), y(pos), z(pos));
        if (r != null) return r;
        if (mundo.getBlockEntity(pos) instanceof ChestBlockEntity) {
            for (BlockPos v : vizinhos(pos)) {
                Armazem.Registro rv = ARMAZEM.porBau(dim, x(v), y(v), z(v));
                if (rv != null && mundo.getBlockEntity(v) instanceof ChestBlockEntity) return rv;
            }
        }
        return null;
    }

    private static boolean dono(PlayerEntity j, Armazem.Registro r) { return r.dono.equals(id(j)); }

    private static boolean podeMexer(PlayerEntity j, Armazem.Registro r) { return dono(j, r) || j.hasPermissionLevel(2); } // OP

    private static UUID id(PlayerEntity j) { return j.getGameProfile().getId(); }

    private static String nome(PlayerEntity j) { return j.getGameProfile().getName(); }

    private static String linha(SignBlockEntity placa, int i) {
        return placa.getFrontText().getMessage(i, false).getString().trim();
    }

    private static MutableText texto(String antes, ItemStack item) {
        MutableText t = Text.literal(antes);
        t.append(item.getName());
        return t;
    }

    private static void msg(PlayerEntity j, String s) { j.sendMessage(Text.literal(s), false); }

    private static String dim(World mundo) { return mundo.getRegistryKey().getValue().toString(); }

    private static int x(BlockPos p) { return p.getX(); }
    private static int y(BlockPos p) { return p.getY(); }
    private static int z(BlockPos p) { return p.getZ(); }

    // =====================================================================  arquivo

    private static void carregar(PlayerEntity jogador) throws Exception {
        MinecraftServer server = jogador.getServer();
        if (server == null) return;
        Path p = server.getSavePath(WorldSavePath.ROOT).resolve("lojas.json");
        if (p.equals(arquivo)) return;
        arquivo = p;
        ARMAZEM.carregar(p);
        List<Armazem.Registro> ruins = new ArrayList<>();
        for (Armazem.Registro r : ARMAZEM.lojas) {
            if (r.privado()) continue;
            try {
                r.vende = ItemStack.fromNbt(StringNbtReader.parse(r.vendeNbt));
                r.recebe = ItemStack.fromNbt(StringNbtReader.parse(r.recebeNbt));
                if (((ItemStack) r.vende).isEmpty() || ((ItemStack) r.recebe).isEmpty()) ruins.add(r);
            } catch (Exception e) {
                ruins.add(r);
            }
        }
        ARMAZEM.lojas.removeAll(ruins);
    }

    private static void salvar() throws Exception {
        if (arquivo != null) ARMAZEM.salvar(arquivo);
    }

    private static void salvarSilencioso() {
        try { salvar(); } catch (Exception e) { System.err.println("[Loja] Não consegui salvar: " + e); }
    }
}
