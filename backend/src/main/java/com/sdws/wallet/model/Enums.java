package com.sdws.wallet.model;

public final class Enums {
    private Enums() {}
    public enum Role { USER, ADMIN }
    public enum TxType { SEND, RECEIVE, ADD, WITHDRAW, TOPUP, BILL }
    public enum TxStatus { SUCCESS, FAILED }
    public enum RequestStatus { PENDING, ACCEPTED, DECLINED }
}
