package de.acomanetopt.manetmodel;

public interface IRadioModel {

    DataRate transmissionBitrate(double distance);

    double receptionPower(double distance);

    boolean interferencePresent(double distance);
}
