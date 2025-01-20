package com.czaplicki.lab06.interfaces;

public interface IOffice {
    int register(String host, String port);
    int order(String host, String port);
    void setReadyToServe(int number);
}