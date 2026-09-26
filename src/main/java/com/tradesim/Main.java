package com.tradesim;
import com.tradesim.http.ApiServer;
import com.tradesim.policy.PolicyEngine;
public final class Main {
    public static void main(String[] args)throws Exception{
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","3000"));
        new ApiServer(new PolicyEngine()).start(port);
        System.err.println("[TradeSim] Server listening on 0.0.0.0:"+port);
    }
}
