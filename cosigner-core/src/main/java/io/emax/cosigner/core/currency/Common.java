package io.emax.cosigner.core.currency;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.emax.cosigner.api.core.CurrencyPackage;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.core.CurrencyParametersRecipient;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.api.currency.Wallet.TransactionDetails;
import io.emax.cosigner.api.validation.Validator;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.cluster.Server;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.CurrencyCommandType;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.atmosphere.cpr.AtmosphereResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscription;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Common {
  private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

  private static HashMap<String, Subscription> balanceSubscriptions = new HashMap<>();
  private static HashMap<String, Subscription> transactionSubscriptions = new HashMap<>();
  private static HashMap<String, Monitor> monitors = new HashMap<>();

  private static CurrencyParameters convertParams(String params) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(params);
      CurrencyParameters currencyParams =
          new ObjectMapper().readValue(jsonParser, CurrencyParameters.class);

      String userKey = currencyParams.getUserKey();
      currencyParams.setUserKey("");
      String sanitizedParams = Json.stringifyObject(CurrencyParameters.class, currencyParams);
      currencyParams.setUserKey(userKey);

      LOGGER.debug("[CurrencyParams] " + sanitizedParams);

      return currencyParams;
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return null;
    }
  }

  private static CurrencyPackage lookupCurrency(CurrencyParameters params) {
    if (CosignerApplication.getCurrencies().containsKey(params.getCurrencySymbol())) {
      return CosignerApplication.getCurrencies().get(params.getCurrencySymbol());
    } else {
      return null;
    }
  }

  /**
   * List all currencies that are currently loaded in cosigner.
   * 
   * @return String list of currencies.
   */
  public static String listCurrencies() {
    List<String> currencies = new LinkedList<>();
    CosignerApplication.getCurrencies().keySet().forEach(currency -> {
      currencies.add(currency);
    });

    String currencyString = Json.stringifyObject(LinkedList.class, currencies);

    LOGGER.debug("[Response] " + currencyString);
    return currencyString;
  }

  /**
   * Registers addresses for currency libraries that need a watch list.
   */
  public static String registerAddress(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    HashMap<String, Boolean> responses = new HashMap<>();
    currencyParams.getAccount().forEach(address -> {
      Boolean result = currency.getWallet().registerAddress(address);
      responses.put(address, result);
    });

    String response = Json.stringifyObject(HashMap.class, responses);
    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * Get a new address for the provided currency & user key.
   * 
   * @param params {@link CurrencyParameters} with the currency code and user key filled in.
   * @return Address that the user can use to deposit funds, for which we can generate the private
   *         keys.
   */
  public static String getNewAddress(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    String userAccount = currency.getWallet().createAddress(currencyParams.getUserKey());
    LinkedList<String> accounts = new LinkedList<>();
    accounts.add(userAccount);
    String userMultiAccount =
        currency.getWallet().getMultiSigAddress(accounts, currencyParams.getUserKey());
    String response = userMultiAccount;

    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * List all addresses that we have generated for the given user key and currency.
   * 
   * @param params {@link CurrencyParameters} with the currency code and user key filled in.
   * @return All addresses that cosigner can generate the private key for belonging to that user
   *         key.
   */
  public static String listAllAddresses(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    LinkedList<String> accounts = new LinkedList<>();
    currency.getWallet().getAddresses(currencyParams.getUserKey()).forEach(accounts::add);
    String response = Json.stringifyObject(LinkedList.class, accounts);

    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * List transactions for the given address and currency.
   * 
   * <p>Will only return data for addresses that belong to cosigner.
   * 
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in.
   * @return List of transactions that affect each account.
   */
  public static String listTransactions(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    currencyParams.getAccount().forEach(account -> {
      txDetails.addAll(Arrays.asList(currency.getWallet().getTransactions(account, 100, 0)));
    });

    String response = Json.stringifyObject(LinkedList.class, txDetails);

    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * Returns the combined balance of all addresses provided in the parameters.
   * 
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in.
   * @return Sum of all balances for the provided addresses.
   */
  public static String getBalance(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    BigDecimal balance = BigDecimal.ZERO;
    if (currencyParams.getAccount() == null || currencyParams.getAccount().isEmpty()) {
      for (String account : currency.getWallet().getAddresses(currencyParams.getUserKey())) {
        balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
      }
    } else {
      for (String account : currencyParams.getAccount()) {
        balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
      }
    }

    String response = balance.toPlainString();

    LOGGER.debug("[Response] " + response);
    return response;
  }

  private static void cleanUpSubscriptions(String id) {
    if (balanceSubscriptions.containsKey(id)) {
      balanceSubscriptions.get(id).unsubscribe();
      balanceSubscriptions.remove(id);
    }

    if (transactionSubscriptions.containsKey(id)) {
      transactionSubscriptions.get(id).unsubscribe();
      transactionSubscriptions.remove(id);
    }

    if (monitors.containsKey(id)) {
      monitors.get(id).destroyMonitor();
      monitors.remove(id);
    }
  }

  /**
   * Sets up a monitor for the given addresses.
   * 
   * <p>A monitor provides periodic balance updates, along with all known transactions when
   * initialized, and any new transactions that come in while it's active. Transactions can be
   * distinguished from balance updates in that the transaction data portion of the response has
   * data, it contains the transaction hash.
   * 
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in. If
   *        using a REST callback, the callback needs to be filled in as well.
   * @param responseSocket If this has been called using a web socket, pass the socket in here and
   *        the data will be written to is as it's available.
   * @return An empty {@link CurrencyParameters} object is returned when the monitor is set up. The
   *         actual data is sent through the socket or callback.
   */
  public static String monitorBalance(String params, AtmosphereResponse responseSocket) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    Monitor monitor = currency.getMonitor().createNewMonitor();

    monitor.addAddresses(currencyParams.getAccount());

    CurrencyParameters returnParms = new CurrencyParameters();
    String response = Json.stringifyObject(CurrencyParameters.class, returnParms);

    // Web socket was passed to us
    if (responseSocket != null) {

      cleanUpSubscriptions(responseSocket.uuid());

      Subscription wsBalanceSubscription = monitor.getObservableBalances().subscribe(balanceMap -> {
        balanceMap.forEach((address, balance) -> {
          try {
            CurrencyParameters responseParms = new CurrencyParameters();
            responseParms.setAccount(new LinkedList<String>());
            responseParms.getAccount().add(address);
            CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
            accountData.setAmount(balance);
            accountData.setRecipientAddress(address);
            responseParms.setReceivingAccount(Arrays.asList(accountData));
            responseSocket.write(Json.stringifyObject(CurrencyParameters.class, responseParms));
          } catch (Exception e) {
            LOGGER.debug(null, e);
            cleanUpSubscriptions(responseSocket.uuid());
            return;
          }
        });
      });
      balanceSubscriptions.put(responseSocket.uuid(), wsBalanceSubscription);

      Subscription wsTransactionSubscription =
          monitor.getObservableTransactions().subscribe(transactionSet -> {
            transactionSet.forEach(transaction -> {
              try {
                CurrencyParameters responseParms = new CurrencyParameters();
                responseParms.setAccount(new LinkedList<String>());
                responseParms.getAccount().addAll(Arrays.asList(transaction.getFromAddress()));
                LinkedList<CurrencyParametersRecipient> receivers = new LinkedList<>();
                Arrays.asList(transaction.getToAddress()).forEach(address -> {
                  CurrencyParametersRecipient sendData = new CurrencyParametersRecipient();
                  sendData.setAmount(transaction.getAmount().toPlainString());
                  sendData.setRecipientAddress(address);
                  receivers.add(sendData);
                });
                responseParms.setReceivingAccount(receivers);
                responseParms.setTransactionData(transaction.getTxHash());
                responseSocket.write(Json.stringifyObject(CurrencyParameters.class, responseParms));
              } catch (Exception e) {
                LOGGER.debug(null, e);
                cleanUpSubscriptions(responseSocket.uuid());
                return;
              }
            });
          });

      transactionSubscriptions.put(responseSocket.uuid(), wsTransactionSubscription);
      monitors.put(responseSocket.uuid(), monitor);
    } else if (currencyParams.getCallback() != null && !currencyParams.getCallback().isEmpty()) {
      // It's a REST callback
      cleanUpSubscriptions(currencyParams.getCallback());

      Subscription rsBalanceSubscription = monitor.getObservableBalances().subscribe(balanceMap -> {
        balanceMap.forEach((address, balance) -> {
          try {
            CurrencyParameters responseParms = new CurrencyParameters();
            responseParms.setAccount(new LinkedList<String>());
            responseParms.getAccount().add(address);
            CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
            accountData.setAmount(balance);
            accountData.setRecipientAddress(address);
            responseParms.setReceivingAccount(Arrays.asList(accountData));

            HttpPost httpPost = new HttpPost(currencyParams.getCallback());
            httpPost.addHeader("content-type", "application/json");
            StringEntity entity;
            entity =
                new StringEntity(Json.stringifyObject(CurrencyParameters.class, responseParms));
            httpPost.setEntity(entity);

            HttpClients.createDefault().execute(httpPost).close();
          } catch (Exception e) {
            LOGGER.debug(null, e);
            cleanUpSubscriptions(currencyParams.getCallback());
            return;
          }
        });
      });
      balanceSubscriptions.put(currencyParams.getCallback(), rsBalanceSubscription);

      Subscription rsTransactionSubscription =
          monitor.getObservableTransactions().subscribe(transactionSet -> {
            transactionSet.forEach(transaction -> {
              try {
                CurrencyParameters responseParms = new CurrencyParameters();
                responseParms.setAccount(new LinkedList<String>());
                responseParms.getAccount().addAll(Arrays.asList(transaction.getFromAddress()));
                LinkedList<CurrencyParametersRecipient> receivers = new LinkedList<>();
                Arrays.asList(transaction.getToAddress()).forEach(address -> {
                  CurrencyParametersRecipient sendData = new CurrencyParametersRecipient();
                  sendData.setAmount(transaction.getAmount().toPlainString());
                  sendData.setRecipientAddress(address);
                  receivers.add(sendData);
                });
                responseParms.setReceivingAccount(receivers);
                responseParms.setTransactionData(transaction.getTxHash());

                HttpPost httpPost = new HttpPost(currencyParams.getCallback());
                httpPost.addHeader("content-type", "application/json");
                StringEntity entity;
                entity =
                    new StringEntity(Json.stringifyObject(CurrencyParameters.class, responseParms));
                httpPost.setEntity(entity);

                HttpClients.createDefault().execute(httpPost).close();
              } catch (Exception e) {
                LOGGER.debug(null, e);
                cleanUpSubscriptions(currencyParams.getCallback());
                return;
              }
            });
          });
      transactionSubscriptions.put(currencyParams.getCallback(), rsTransactionSubscription);
      monitors.put(currencyParams.getCallback(), monitor);
    } else {
      // We have no way to respond to the caller other than with this response.
      monitor.destroyMonitor();
    }

    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * Create and sign a transaction.
   * 
   * <p>This only signs the transaction with the user's key, showing that the user has requested the
   * transaction. The server keys are not used until the approve stage.
   * 
   * @param params {@link CurrencyParameters} with the currency code, user key, senders, recipients
   *        and amounts filled in.
   * @return The transaction string that was requested.
   */
  public static String prepareTransaction(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    CurrencyPackage currency = lookupCurrency(currencyParams);

    // Create the transaction
    List<String> addresses = new LinkedList<>();
    addresses.addAll(currencyParams.getAccount());
    LinkedList<Recipient> recipients = new LinkedList<>();
    currencyParams.getReceivingAccount().forEach(account -> {
      Recipient recipient = new Recipient();
      recipient.setAmount(new BigDecimal(account.getAmount()));
      recipient.setRecipientAddress(account.getRecipientAddress());
      recipients.add(recipient);
    });
    currencyParams
        .setTransactionData(currency.getWallet().createTransaction(addresses, recipients));

    // Authorize it with the user account
    String initalTx = currencyParams.getTransactionData();

    // Try to validate it, don't sign if it fails.
    for (Validator validator : CosignerApplication.getValidators()) {
      if (!validator.validateTransaction(currency, initalTx)) {
        return initalTx;
      }
    }

    currencyParams.setTransactionData(currency.getWallet().signTransaction(initalTx,
        currencyParams.getAccount().get(0), currencyParams.getUserKey()));

    // If the userKey/address combo don't work then we stop here.
    if (currencyParams.getTransactionData().equalsIgnoreCase(initalTx)) {
      return initalTx;
    }

    // Send it if it's a sign-each and there's more than one signature
    // required (we're at 1/X)
    if (currency.getConfiguration().getMinSignatures() > 1
        && currency.getConfiguration().getSigningType().equals(SigningType.SENDEACH)) {
      submitTransaction(Json.stringifyObject(CurrencyParameters.class, currencyParams));
    }

    String response = currencyParams.getTransactionData();
    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * Approve a transaction that's been signed off on by the user.
   * 
   * <p>This stage signs the transaction with the server keys after running it through any sanity
   * checks and validation required.
   * 
   * @param params {@link CurrencyParameters} with the currency code, user key, senders, recipients
   *        and amounts filled in. The transaction data should be filled in with the response from
   *        prepareTransaction.
   * @param sendToRemotes Indicates whether cosigner should attempt to request signature from any
   *        other cosigner servers in the cluster.
   * @return Signed transaction string
   */
  public static String approveTransaction(String params, boolean sendToRemotes) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    for (Server server : ClusterInfo.getInstance().getServers()) {
      if (server.isOriginator()) { // It's us, try to sign it locally.
        // But first check that it's valid.
        for (Validator validator : CosignerApplication.getValidators()) {
          if (!validator.validateTransaction(currency, currencyParams.getTransactionData())) {
            return currencyParams.getTransactionData();
          }
        }
        currencyParams.setTransactionData(currency.getWallet().signTransaction(
            currencyParams.getTransactionData(), currencyParams.getAccount().get(0)));
      } else if (sendToRemotes) {
        try {
          CurrencyCommand command = new CurrencyCommand();
          command.setCurrencyParams(currencyParams);
          command.setCommandType(CurrencyCommandType.SIGN);
          command =
              CurrencyCommand.parseCommandString(Coordinator.broadcastCommand(command, server));

          if (command != null) {
            String originalTx = currencyParams.getTransactionData();
            currencyParams.setTransactionData(command.getCurrencyParams().getTransactionData());

            // If it's send-each and the remote actually signed it, send it.
            if (!originalTx.equalsIgnoreCase(currencyParams.getTransactionData())
                && currency.getConfiguration().getSigningType().equals(SigningType.SENDEACH)) {
              submitTransaction(Json.stringifyObject(CurrencyParameters.class, currencyParams));
            }
          }
        } catch (Exception e) {
          // Likely caused by an offline server or bad response.
          LOGGER.warn(null, e);
        }
      }
    }
    String response = currencyParams.getTransactionData();

    LOGGER.debug("[Response] " + response);
    return response;
  }

  /**
   * Submits a transaction for processing on the network.
   * 
   * @param params {@link CurrencyParameters} with the currency and transaction data filled in. The
   *        transaction data required is the result from the approveTransaction stage.
   * @return The transaction hash/ID.
   */
  public static String submitTransaction(String params) {
    CurrencyParameters currencyParams = convertParams(params);
    CurrencyPackage currency = lookupCurrency(currencyParams);

    String response = currency.getWallet().sendTransaction(currencyParams.getTransactionData());

    LOGGER.debug("[Response] " + response);
    return response;
  }

}
