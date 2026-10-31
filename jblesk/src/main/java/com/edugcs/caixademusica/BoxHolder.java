package com.edugcs.caixademusica;

/**
 * A caixa embutida no baú de um burro, mula, lhama ou barco com baú: fica guardada na própria entidade (e salva
 * junto com ela, veja o mixin {@code EntityMixin}). Só o servidor mexe nisto; os clientes recebem o estado pelo
 * {@link PortableTracker}. Toda entidade é um {@code BoxHolder}; só as de {@link PortableTracker#isSpeakerCarrier}
 * usam.
 */
public interface BoxHolder {
    /** A caixa desta entidade, ou {@code null} se ainda não foi usada. */
    PlaybackState caixademusica$getBox();

    void caixademusica$setBox(PlaybackState state);
}
