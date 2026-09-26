package com.tradesim.http;
import com.sun.net.httpserver.*;
import com.tradesim.model.*;
import com.tradesim.policy.PolicyEngine;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
public final class ApiServer {
    private final PolicyEngine engine;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;
    public ApiServer(PolicyEngine engine){this.engine=engine;}
    public void start(int port)throws IOException{
        server=HttpServer.create(new InetSocketAddress("0.0.0.0",port),0);
        server.createContext("/api/state",x->handle(x,"GET"));
        server.createContext("/api/backtest",x->handle(x,"POST"));
        server.createContext("/api/reset",x->handle(x,"POST"));
        server.createContext("/",this::staticFile); executor=java.util.concurrent.Executors.newFixedThreadPool(2); server.setExecutor(executor); server.start();
    }
    public void stop(){if(server!=null)server.stop(0);if(executor!=null)executor.shutdownNow();}
    private void handle(HttpExchange x,String method)throws IOException{
        try{
            if(!x.getRequestMethod().equals(method)){send(x,405,"{\"error\":\"Method not allowed\"}");return;}
            String path=x.getRequestURI().getPath();
            if(path.equals("/api/state")){send(x,200,state());return;}
            if(path.equals("/api/reset")){engine.reset();send(x,200,"{\"status\":\"reset_ok\"}");return;}
            Map<String,String> body=parse(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            String ticker=body.getOrDefault("ticker","ACME_TECH"), strategy=body.getOrDefault("strategy","all");
            int limit=integer(body,"maxTransactions",2), cooldown=integer(body,"cooldownDays",1); double fee=decimal(body,"transactionFee",2);
            TradingRules rules=new TradingRules(limit,fee,cooldown);
            var data=new com.tradesim.data.MarketDataRepository().get(ticker);
            List<BacktestResult> results=strategy.equals("all")?engine.compare(ticker,rules):List.of(engine.runBacktest(ticker,strategy,rules));
            send(x,200,serialize(ticker,data.prices(),results));
        }catch(IllegalArgumentException e){send(x,400,"{\"error\":\"Invalid request\",\"message\":"+q(e.getMessage())+"}");}
        catch(Exception e){System.err.println("[TradeSim] "+e);send(x,500,"{\"error\":\"Backtest failed\",\"message\":\"The request could not be completed\"}");}
    }
    private void staticFile(HttpExchange x)throws IOException{
        String path=x.getRequestURI().getPath(); if(path.equals("/"))path="/index.html";
        if(path.contains("..")){send(x,400,"Bad path");return;}
        try(InputStream in=getClass().getResourceAsStream("/web"+path)){if(in==null){send(x,404,"Not found");return;}
            byte[] bytes=in.readAllBytes();String type=path.endsWith(".css")?"text/css":path.endsWith(".js")?"application/javascript":"text/html";
            x.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes);
        }
    }
    private String state(){return "{\"application\":\"TradeSim\",\"version\":\"1.0\",\"tickers\":[\"ACME_TECH\",\"NOVA\",\"STEEL\"],\"strategies\":[\"single\",\"unlimited\",\"limited\",\"fee\",\"cooldown\",\"combined\"]}";}
    private String serialize(String ticker,List<Double> prices,List<BacktestResult> results){
        StringBuilder b=new StringBuilder("{\"ticker\":").append(q(ticker)).append(",\"prices\":").append(nums(prices)).append(",\"results\":[");
        for(int i=0;i<results.size();i++){if(i>0)b.append(',');BacktestResult r=results.get(i);var t=r.telemetry();
            b.append("{\"strategyId\":").append(q(id(r.strategy()))).append(",\"strategy\":").append(q(r.strategy())).append(",\"profit\":").append(n(r.totalProfit())).append(",\"trades\":").append(r.transactionCount()).append(",\"maxDrawdown\":").append(n(r.maxDrawdown())).append(",\"status\":").append(q(r.status())).append(",\"transactions\":[");
            for(int j=0;j<r.transactions().size();j++){if(j>0)b.append(',');Trade z=r.transactions().get(j);b.append("{\"action\":").append(q(z.action())).append(",\"day\":").append(z.day()).append(",\"price\":").append(n(z.price())).append(",\"fee\":").append(n(z.fee())).append(",\"realizedProfit\":").append(n(z.realizedProfit())).append('}');}
            b.append("],\"telemetry\":{\"daysProcessed\":").append(t.daysProcessed()).append(",\"buySignals\":").append(t.buySignals()).append(",\"sellSignals\":").append(t.sellSignals()).append(",\"feesPaid\":").append(n(t.feesPaid())).append(",\"cooldownDaysObserved\":").append(t.cooldownDaysObserved()).append(",\"peakPortfolioValue\":").append(n(t.peakPortfolioValue())).append(",\"finalPortfolioValue\":").append(n(t.finalPortfolioValue())).append(",\"grossProfit\":").append(n(t.grossProfit())).append(",\"netProfit\":").append(n(t.netProfit())).append("}}");}
        return b.append("]}").toString();
    }
    private String id(String n){return switch(n){case "Single Execution Cycle"->"single";case "Unlimited Execution Policy"->"unlimited";case "Transaction-Limited Policy"->"limited";case "Fee-Aware Execution Policy"->"fee";case "Cooldown Execution Policy"->"cooldown";default->"combined";};}
    private String nums(List<Double> vs){return "["+String.join(",",vs.stream().map(ApiServer::n).toList())+"]";}
    private static String n(double v){return String.format(Locale.US,"%.2f",v);}
    private static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    private void send(HttpExchange x,int code,String text)throws IOException{byte[] data=text.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(code,data.length);x.getResponseBody().write(data);x.close();}
    private Map<String,String> parse(String s){String json=s.trim();if(!json.startsWith("{")||!json.endsWith("}"))throw new IllegalArgumentException("Malformed JSON request");Map<String,String> m=new HashMap<>();Matcher a=Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|[-+]?[0-9]*\\.?[0-9]+|true|false|null)").matcher(json);while(a.find())m.put(a.group(1),a.group(3)!=null?a.group(3):a.group(2));if(m.isEmpty()&&!json.equals("{}"))throw new IllegalArgumentException("Malformed JSON request");return m;}
    private int integer(Map<String,String> m,String k,int d){try{return Integer.parseInt(m.getOrDefault(k,""+d));}catch(Exception e){throw new IllegalArgumentException(k+" must be an integer");}}
    private double decimal(Map<String,String> m,String k,double d){try{return Double.parseDouble(m.getOrDefault(k,""+d));}catch(Exception e){throw new IllegalArgumentException(k+" must be numeric");}}
}
