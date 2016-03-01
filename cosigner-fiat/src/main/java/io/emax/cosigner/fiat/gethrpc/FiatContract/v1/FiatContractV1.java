package io.emax.cosigner.fiat.gethrpc.FiatContract.v1;

import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractInterface;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractParametersInterface;

public class FiatContractV1 implements FiatContractInterface {
  private static final String initData =
      "60606040526040516108a63803806108a683398101604052805160805160a051"
          + "9192019060006001819055601281905560058054600160a060020a0319168517"
          + "90555b82518110156100da578281815181101561000257906020019060200201"
          + "516008600050600083600101815260200190815260200160002060006101000a"
          + "815481600160a060020a03021916908302179055508060010160076000506000"
          + "858481518110156100025790602001906020020151600160a060020a03168152"
          + "60200190815260200160002060005081905550600101610042565b8251600460"
          + "00508190555081600060005081905550505050506107a5806101016000396000" + "f3";
  private static final String contractPayload =
      "606060405236156100615760e060020a600035046312b58349811461006a5780"
          + "632f54bf6e1461008057806367fbd289146100a65780637e1f2bb81461013157"
          + "8063a0e67e2b1461017a578063f8b2cb4f14610209578063fd764e681461022d"
          + "575b6103255b610002565b6012545b60408051918252519081900360200190f3"
          + "5b61006e600435600160a060020a038116600090815260076020526040812054"
          + "115b919050565b610325600435600554600160a060020a031660009081526011"
          + "60205260409020548190108015906100fd5750600560009054906101000a9004"
          + "600160a060020a0316600160a060020a031633600160a060020a0316145b1561"
          + "006557600554600160a060020a03166000908152601160205260409020805482"
          + "90039055601280548290039055610177565b61032560043560055433600160a0"
          + "60020a039081169116141561017757600554600160a060020a03166000908152"
          + "60116020526040902080548201905560128054820190555b50565b6040805160"
          + "2081810183526000808352835191820184528082526004549351610327949080"
          + "59106101a85750595b9080825280602002602001820160405250915060009050"
          + "5b60045481101561060e57600181016000908152600860205260409020548251"
          + "600160a060020a03919091169083908390811015610002576020908102909101"
          + "01526001016101c0565b61006e600435600160a060020a038116600090815260"
          + "1160205260409020546100a1565b610325600480359060248035916044358083"
          + "0192908201359160643580830192908201359160843580830192908201359160"
          + "a43580830192908201359160c435918201910135600a805473ffffffffffffff"
          + "ffffffffffffffffffffffffff19168c179055600d89905560005b600d548110"
          + "15610371578a8a8281811015610002576000848152600e602090815260409091"
          + "20805473ffffffffffffffffffffffffffffffffffffffff1916929091029093"
          + "013517909155508888828181101561000257905090906020020135600e600050"
          + "6000838152602001908152602001600020600050600101600050819055506001"
          + "0161029a565b005b604051808060200182810382528381815181526020019150"
          + "80519060200190602002808383829060006004602084601f0104600f02600301"
          + "f1509050019250505060405180910390f35b5060098c9055600f85905560005b"
          + "600f5481101561042e5786868281811015610002576000848152601060209081"
          + "526040909120805460ff19169290910290930135179091555084848281811015"
          + "6100025790509090602002013560106000506000838152602001908152602001"
          + "6000206000506001016000508190555082828281811015610002579050909060"
          + "2002013560106000506000838152602001908152602001600020600050600201"
          + "6000508190555060010161037f565b6104bf6000600c819055600b8190556006"
          + "819055808080805b600d5484101561062057600c80546000868152600e602090"
          + "81526040918290208054600191909101546009548451958652600160a060020a"
          + "03929092166c0100000000000000000000000002928501929092526034840191"
          + "909152605483015251908190036074019020905560019390930192610447565b"
          + "80156104cd5750600b546001145b156105ff575060095460015560005b600d54"
          + "8110156105ff576000818152600e6020908152604080832060010154600a5460"
          + "0160a060020a031684526011909252909120541061006557600e600050600082"
          + "8152602001908152602001600020600050600101600050546011600050600060"
          + "0a60009054906101000a9004600160a060020a0316600160a060020a03168152"
          + "602001908152602001600020600082828250540392505081905550600e600050"
          + "6000828152602001908152602001600020600050600101600050546011600050"
          + "6000600e60005060008581526020019081526020016000206000506000016000"
          + "9054906101000a9004600160a060020a0316600160a060020a03168152602001"
          + "9081526020016000206000828282505401925050819055506001016104dc565b"
          + "50505050505050505050505050565b50919050565b600094505b505050509056"
          + "5b600180546009549101146106375760009450610619565b6000600281905560"
          + "0381905593505b600f54841080156106575750600884105b156106e257600c54"
          + "6000858152601060209081526040808320805460018281015460029390930154"
          + "845197885260ff92909216878601528684019290925260608601529051909360"
          + "80818101949183900301908290866161da5a03f1156100025750506040515192"
          + "506106f683600160a060020a0381166000908152600760205260409020546100"
          + "a1565b600054600354106106145760019450610619565b91508160020a905060"
          + "0082118015610712575060025481166000145b1561072d576002805482179055"
          + "600380546001019055610799565b600a54600160a060020a0384811691161480"
          + "1561074c5750600b546000145b1561076557600380546001908101909155600b"
          + "55610799565b600554600160a060020a03848116911614801561078457506006"
          + "546000145b15610799576003805460019081019091556006555b600193909301" + "9261064656";

  /* Constructor: fiatcontract(address _admin, address[] _owners, uint _required) */

  /*  7e1f2bb8 createTokens(uint256) // Deposits in admin account for transfer to new owner.
      67fbd289 destroyTokens(uint256) // Destroys from the admin account.
      f8b2cb4f getBalance(address)
      a0e67e2b getOwners()
      12b58349 getTotalBalance()
      2f54bf6e isOwner(address)
      fd764e68 transfer(uint256 nonce, address sender, address[] to, uint256[] value,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) */

  private static final String createTokens = "7e1f2bb8";
  private static final String destroyTokens = "67fbd289";
  private static final String getBalance = "f8b2cb4f";
  private static final String getOwners = "a0e67e2b";
  private static final String getTotalBalance = "12b58349";
  private static final String isOwner = "2f54bf6e";
  private static final String transfer = "fd764e68";

  @Override
  public String getCreateTokens() {
    return createTokens;
  }

  @Override
  public String getDestroyTokens() {
    return destroyTokens;
  }

  @Override
  public String getGetBalance() {
    return getBalance;
  }

  @Override
  public String getGetOwners() {
    return getOwners;
  }

  @Override
  public String getGetTotalBalance() {
    return getTotalBalance;
  }

  @Override
  public String getIsOwner() {
    return isOwner;
  }

  @Override
  public String getTransfer() {
    return transfer;
  }

  @Override
  public String getInitData() {
    return initData;
  }

  @Override
  public String getContractPayload() {
    return contractPayload;
  }

  @Override
  public FiatContractParametersInterface getContractParameters() {
    return new FiatContractParametersV1();
  }
}