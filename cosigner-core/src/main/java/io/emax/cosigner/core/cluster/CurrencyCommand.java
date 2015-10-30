package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.core.currency.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CurrencyCommand implements BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(CurrencyCommand.class);
  private CurrencyCommandType commandType;
  private CurrencyParameters currencyParams;

  public CurrencyCommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(CurrencyCommandType commandType) {
    this.commandType = commandType;
  }

  public CurrencyParameters getCurrencyParams() {
    return currencyParams;
  }

  public void setCurrencyParams(CurrencyParameters currencyParams) {
    this.currencyParams = currencyParams;
  }

  /**
   * Convert this command to a JSON string.
   */
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(CurrencyCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return "";
    }
  }

  /**
   * Parse a JSON string that represents a CurrencyCommand.
   * 
   * @param commandString JSON string to parse.
   * @return CurrencyCommand that the string represents.
   */
  public static CurrencyCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      CurrencyCommand currencyCommand =
          new ObjectMapper().readValue(jsonParser, CurrencyCommand.class);
      return currencyCommand;
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return null;
    }
  }

  /**
   * Attempt to handle the request in a currency command.
   * 
   * @param command The command received.
   * @return The string response to return to the requestor.
   */
  public static String handleCommand(CurrencyCommand command) {
    // SIGN -- Attempt to sign the provided data, for off-client keys
    switch (command.commandType) {
      case SIGN:
        String signedTx = Common.approveTransaction(
            Common.stringifyObject(CurrencyParameters.class, command.currencyParams), false);
        command.currencyParams.setTransactionData(signedTx);
        return command.toJson();
      default:
        return command.toJson();
    }
  }
}
