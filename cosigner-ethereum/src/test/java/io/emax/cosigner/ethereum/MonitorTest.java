package io.emax.cosigner.ethereum;

import io.emax.cosigner.ethereum.stubrpc.EthereumTestRpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rx.Subscription;

import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorTest {
  private EthereumWallet wallet;
  private EthereumMonitor monitor;
  private String userKey;
  private int balanceCounter;

  @Before
  public void setUp() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    wallet = new EthereumWallet(new EthereumConfiguration());
    monitor = new EthereumMonitor(wallet);
    userKey = "deadbeef";
    balanceCounter = 0;
  }

  @Test
  public void testMonitor() throws InterruptedException {
    System.out.println("");
    System.out.println("Simple monitor test");
    // Get addresses
    String singleAddress = wallet.createAddress(userKey);
    System.out.println("Single Address Test: " + singleAddress);
    String multiAddress =
        wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
    System.out.println("Multi Address Test: " + multiAddress);
    monitor.addAddresses(Collections.singletonList(multiAddress));

    singleAddress = wallet.createAddress(userKey);
    System.out.println("Single Address Test: " + singleAddress);
    multiAddress = wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
    System.out.println("Multi Address Test: " + multiAddress);
    monitor.addAddresses(Collections.singletonList(multiAddress));

    System.out.println("Waiting for balance updates (2 minutes)... ");
    System.out.println("Time: " + new Date());

    Subscription monitorSub = monitor.getObservableBalances().subscribe(addresses -> {
      System.out.println("Time: " + new Date());
      addresses.forEach((address, balance) -> {
        balanceCounter++;
        System.out.println("Address: " + address + " Balance: " + balance);
      });
    });

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("Unsubscribing from monitor. Test should complete within 10 seconds.");
        System.out.println("Time: " + new Date());
        monitorSub.unsubscribe();
      }
    }, 2 * 60 * 1000);

    while (!monitorSub.isUnsubscribed()) {
      Thread.sleep(10 * 1000);
    }

    Assert.assertTrue(balanceCounter > 0);
  }
}
