package net.authorize.cim.functional_test;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import net.authorize.ResponseField;
import net.authorize.Transaction;
import net.authorize.UnitTestData;
import net.authorize.aim.functional_test.MultiOrderAuth_Capture_Void_CreditTest;
import net.authorize.cim.Result;
import net.authorize.cim.SplitTenderStatus;
import net.authorize.cim.TransactionType;
import net.authorize.cim.ValidationModeType;
import net.authorize.data.Order;
import net.authorize.data.OrderItem;
import net.authorize.data.ShippingCharges;
import net.authorize.data.cim.CustomerProfile;
import net.authorize.data.cim.PaymentProfile;
import net.authorize.data.cim.PaymentTransaction;
import net.authorize.data.creditcard.AVSCode;
import net.authorize.data.creditcard.CreditCard;
import net.authorize.data.echeck.BankAccountType;
import net.authorize.data.echeck.ECheckType;
import net.authorize.data.xml.Address;
import net.authorize.data.xml.BankAccount;
import net.authorize.data.xml.Customer;
import net.authorize.data.xml.CustomerType;
import net.authorize.data.xml.Payment;
import net.authorize.util.XmlUtility;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

public class CIMTest extends UnitTestData {

	private static String customerProfileId = null;
	private static String customerPaymentProfileId = null;
	private static String customerShippingAddressId = null;
	private static String authCode = null;
	private static String transactionId = null;
	private static String splitTenderId = null;

	String refId;
	CreditCard creditCard;
	BankAccount bankAccount;
	Address billingInfo;
	Address shippingInfo;
	Customer customer;
	CustomerProfile customerProfile;
	Order order;
	PaymentProfile paymentProfileCC;
	PaymentProfile paymentProfileBankAccount;
	Payment payment;
	PaymentTransaction paymentTransaction;

	@Before
	public void setUp() throws Exception {
		refId = "REFID:" + System.currentTimeMillis();
		// Create a payment profile
		//
		paymentProfileCC = PaymentProfile.createPaymentProfile();
		paymentProfileBankAccount = PaymentProfile.createPaymentProfile();

		// Create a new credit card
		//
		creditCard = CreditCard.createCreditCard();
		creditCard.setCreditCardNumber(creditCardNumber);
		creditCard.setExpirationDate("2029-07");
		creditCard.setCardCode("123");
		creditCard.setAvsCode(AVSCode.Z);

		// Create a bank account
		bankAccount = BankAccount.createBankAccount();
		bankAccount.setBankAccountName(bankAccountName);
		bankAccount.setBankAccountNumber(bankAccountNumber);
		bankAccount.setBankAccountType(BankAccountType.BUSINESSCHECKING);
		bankAccount.setBankName(bankName);
		bankAccount.setRoutingNumber(routingNumber);
		bankAccount.setECheckType(ECheckType.CCD);

		// Create a billing info
		//
		billingInfo = (Address) Address.createAddress();
		billingInfo.setFirstName(firstName + System.currentTimeMillis());
		billingInfo.setLastName(lastName);
		billingInfo.setCompany(company);
		billingInfo.setAddress(address);
		billingInfo.setCity(city);
		billingInfo.setState(state);
		billingInfo.setCountry(country);
		billingInfo.setZipPostalCode(zipPostalCode);
		billingInfo.setPhoneNumber(phone);
		billingInfo.setFaxNumber(fax);

		// Create a shipping info
		//
		shippingInfo = (Address) Address.createAddress();
		shippingInfo.setFirstName(firstName + System.currentTimeMillis());
		shippingInfo.setLastName(lastName);
		shippingInfo.setCompany(company);
		shippingInfo.setAddress(address);
		shippingInfo.setCity(city);
		shippingInfo.setState(state);
		shippingInfo.setCountry(country);
		shippingInfo.setZipPostalCode(zipPostalCode);
		shippingInfo.setPhoneNumber(phone);
		shippingInfo.setFaxNumber(fax);

		// Create a customer and specify billing info
		//
		customer = Customer.createCustomer();
		customer.setBillTo(billingInfo);
		customer.setShipTo(shippingInfo);
		customer.setEmail(email);
		customer.setPhoneNumber(phone);

		order = Order.createOrder();
		order.setDescription(orderDescription);
		order.setInvoiceNumber(Long.toString(System.currentTimeMillis()));
		order.setTotalAmount(totalAmount);

		// add info
		paymentProfileCC.setBillTo(billingInfo);
		paymentProfileCC.addPayment(Payment.createPayment(creditCard));
		
		paymentProfileCC.setCustomerType(CustomerType.INDIVIDUAL);

		paymentProfileBankAccount.setCustomerType(CustomerType.INDIVIDUAL);
		paymentProfileBankAccount.setBillTo(billingInfo);
		paymentProfileBankAccount.addPayment(Payment.createPayment(bankAccount));
		
		
		
		customerProfile = CustomerProfile.createCustomerProfile();
		customerProfile.setDescription(customerDescription);
		customerProfile.setMerchantCustomerId("" + System.currentTimeMillis());
		customerProfile.setEmail(email);

		// create order item
		OrderItem orderItem = OrderItem.createOrderItem();
		orderItem.setItemDescription(itemDescription);
		orderItem.setItemId(itemId);
		orderItem.setItemName(itemName);
		orderItem.setItemPrice(itemPrice);
		orderItem.setItemQuantity(itemQuantity);
		orderItem.setItemTaxable(true);
		order.addOrderItem(orderItem);

		orderItem = OrderItem.createOrderItem();
		orderItem.setItemDescription(itemDescription2);
		orderItem.setItemId(itemId2);
		orderItem.setItemName(itemName2);
		orderItem.setItemPrice(itemPrice2);
		orderItem.setItemQuantity(itemQuantity2);
		orderItem.setItemTaxable(false);
		order.addOrderItem(orderItem);

		for (int i = 0; i <= 2; i++) {
			order.addOrderItem(orderItem);
		}

		// shipping charges
		ShippingCharges shippingCharges = ShippingCharges
				.createShippingCharges();
		shippingCharges.setDutyAmount(dutyAmount);
		shippingCharges.setDutyItemDescription(dutyItemDescription);
		shippingCharges.setDutyItemName(dutyItemName);
		shippingCharges.setFreightAmount(freightAmount);
		shippingCharges.setFreightDescription(freightDescription);
		shippingCharges.setFreightItemName(freightItemName);
		shippingCharges.setPurchaseOrderNumber(purchaseOrderNumber);
		shippingCharges.setTaxAmount(taxAmount);
		shippingCharges.setTaxDescription(taxDescription);
		shippingCharges.setTaxExempt(taxExempt);
		shippingCharges.setTaxItemName(taxItemName);
		order.setShippingCharges(shippingCharges);

		paymentTransaction = PaymentTransaction.createPaymentTransaction();
		paymentTransaction.setOrder(order);
		paymentTransaction.setCardCode(cardholderAuthenticationValue);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateAndDeleteCustomerProfile() {
		//create Customer profile
		Result<Transaction> result = createCustomerProfile(customerProfile, paymentProfileCC, ValidationModeType.TEST_MODE);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertNotNull(result.getCustomerProfileId());
		Assert.assertNotNull(result.getCustomerPaymentProfileIdList());
		Assert.assertTrue(result.getCustomerPaymentProfileIdList().size() == 1);
		Assert.assertTrue(result.getDirectResponseList().size() == 1);

		// delete a customer profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.DELETE_CUSTOMER_PROFILE);
		setRefId(transaction);
		transaction.setCustomerProfileId(result.getCustomerProfileId());
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateAndDeleteCustomerPaymentProfileRequestBankAccount() {

		Result<Transaction> result = createCustomerProfile(customerProfile, paymentProfileCC, ValidationModeType.TEST_MODE);
		String customerProfileId = result.getCustomerProfileId();
		
		// Create a new customer payment profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PAYMENT_PROFILE);
		setRefId(transaction);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.addPaymentProfile(paymentProfileBankAccount);
		transaction.setValidationMode(ValidationModeType.TEST_MODE);
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		ArrayList<String> paymentList = result.getCustomerPaymentProfileIdList();
		Assert.assertNotNull(paymentList);
		Assert.assertTrue(paymentList.size() > 0);
		Assert.assertEquals(1, result.getDirectResponseList().size());

		customerPaymentProfileId = paymentList.get(0);

		// delete a customer payment profile
		transaction = merchant.createCIMTransaction(TransactionType.DELETE_CUSTOMER_PAYMENT_PROFILE);
		setRefId(transaction);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(paymentList.get(0));
		transaction.setValidationMode(ValidationModeType.TEST_MODE);
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerPaymentProfileRequestCC() {

		Result<Transaction> result = createCustomerProfile(customerProfile, paymentProfileBankAccount, ValidationModeType.TEST_MODE);

		// Create a new customer payment profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PAYMENT_PROFILE);
		setRefId(transaction);
		transaction.setCustomerProfileId(result.getCustomerProfileId());
		transaction.addPaymentProfile(paymentProfileCC);
		transaction.setValidationMode(ValidationModeType.TEST_MODE);
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getCustomerPaymentProfileIdList().size() > 0);
		Assert.assertEquals(1, result.getDirectResponseList().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateAndDeleteCustomerShippingAddress() {

		Result<Transaction> result = createCustomerProfile(customerProfile, paymentProfileCC, ValidationModeType.TEST_MODE);
		String customerProfileId = result.getCustomerProfileId();

		// Create a new shipping address profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.CREATE_CUSTOMER_SHIPPING_ADDRESS);
		setRefId(transaction);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setShipTo(shippingInfo);
		transaction.setValidationMode(ValidationModeType.TEST_MODE);
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		
		ArrayList<String> shippingAddressIdList = result.getCustomerShippingAddressIdList();
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertNotNull(shippingAddressIdList);
		Assert.assertTrue(shippingAddressIdList.size() > 0);
		Assert.assertEquals(1, shippingAddressIdList.size());

 		// delete the shipping address
		transaction = merchant.createCIMTransaction(TransactionType.DELETE_CUSTOMER_SHIPPING_ADDRESS);
		setRefId(transaction);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerShippingAddressId(shippingAddressIdList.get(0));
		transaction.setValidationMode(ValidationModeType.TEST_MODE);
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_AuthOnly_NoCardCode() {
		// Create an auth only txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.AUTH_ONLY);
		transaction.setPaymentTransaction(paymentTransaction);
		transaction.setCardCode(null);
		transaction.addExtraOption("ip_address", "127.0.0.1");

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
		authCode = result.getDirectResponseList().get(0).getDirectResponseMap()
				.get(ResponseField.AUTHORIZATION_CODE);
		splitTenderId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.SPLIT_TENDER_ID);
		transactionId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.TRANSACTION_ID);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_AuthOnly() {
		// Create an auth only txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.AUTH_ONLY);
		transaction.setPaymentTransaction(paymentTransaction);
		transaction.addExtraOption("ip_address", "127.0.0.1");

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
		authCode = result.getDirectResponseList().get(0).getDirectResponseMap()
				.get(ResponseField.AUTHORIZATION_CODE);
		splitTenderId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.SPLIT_TENDER_ID);
		transactionId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.TRANSACTION_ID);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_PriorAuthCapture() {
		// Create an auth capture txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.PRIOR_AUTH_CAPTURE);
		paymentTransaction.setTransactionId(transactionId);
		transaction.setPaymentTransaction(paymentTransaction);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_AuthCapture() {
		// Create an auth capture txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.AUTH_CAPTURE);
		transaction.setPaymentTransaction(paymentTransaction);
		transaction.addExtraOption("ip_address", "127.0.0.1");

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		transactionId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.TRANSACTION_ID);
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_CaptureOnly() {
		testCreateCustomerProfileTransactionRequest_AuthOnly(); // get a new
																// auth
		// Create a capture only txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.CAPTURE_ONLY);
		paymentTransaction.setApprovalCode(authCode);
		transaction.setPaymentTransaction(paymentTransaction);
		transaction.addExtraOption("ip_address", "127.0.0.1");

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
		transactionId = result.getDirectResponseList().get(0)
				.getDirectResponseMap().get(ResponseField.TRANSACTION_ID);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateCustomerProfileTransactionRequest_Void() {
		// Create a void txn request
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE_TRANSACTION);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		paymentTransaction.setOrder(null);
		paymentTransaction
				.setTransactionType(net.authorize.TransactionType.VOID);
		paymentTransaction.setTransactionId(transactionId);
		transaction.setPaymentTransaction(paymentTransaction);
		transaction.addExtraOption("ip_address", "127.0.0.1");

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertTrue(result.getDirectResponseList().size() > 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetCustomerProfileIdsRequest() {
		// get customer profile ids
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_PROFILE_IDS);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertTrue(result.getCustomerProfileIdList().size() > 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetCustomerProfileRequest() {
		// get a customer profile
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_PROFILE);

		 transaction.setCustomerProfileId(customerProfileId);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerProfileId());
		Assert.assertEquals(customerProfileId, result.getCustomerProfileId());
		Assert.assertNotNull(result.getCustomerProfile());
		Assert.assertEquals(customerDescription, result.getCustomerProfile()
				.getDescription());
		Assert.assertEquals(email, result.getCustomerProfile().getEmail());		
		Assert.assertNotNull(result.getCustomerProfile().getCustomerProfileId());
		Assert.assertNotNull(result.getCustomerProfile().getShipToAddressList());
		Assert.assertTrue(result.getCustomerProfile().getShipToAddressList()
				.size() > 0);
		Assert.assertNotNull(result.getCustomerPaymentProfileIdList());
		Assert.assertTrue(result.getCustomerPaymentProfileIdList().size() == 3);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetCustomerPaymentProfileRequest() {
		// get a customer payment profile
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_PAYMENT_PROFILE);

		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);
		Assert.assertEquals(phone, result.getCustomerPaymentProfile().getBillTo().getPhoneNumber());
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerPaymentProfile());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetCustomerShippingAddressRequest() {
		// get a customer shipping address
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_SHIPPING_ADDRESS);

		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerProfile());
		Assert.assertNotNull(result.getCustomerProfile().getShipToAddressList());
		Assert.assertEquals(1, result.getCustomerProfile()
				.getShipToAddressList().size());
		Address shippingAddress = result.getCustomerProfile()
				.getShipToAddressList().get(0);
		Assert.assertNotNull(shippingAddress);
		Assert.assertThat(shippingAddress.getFirstName(),
				JUnitMatchers.containsString(firstName));
		Assert.assertEquals(lastName, shippingAddress.getLastName());
		Assert.assertEquals(company, shippingAddress.getCompany());
		Assert.assertEquals(address, shippingAddress.getAddress());
		Assert.assertEquals(city, shippingAddress.getCity());
		Assert.assertEquals(state, shippingAddress.getState());
		Assert.assertEquals(zipPostalCode, shippingAddress.getZipPostalCode());
		Assert.assertEquals(country, shippingAddress.getCountry());
		Assert.assertEquals(phone, shippingAddress.getPhoneNumber());
		Assert.assertEquals(fax, shippingAddress.getFaxNumber());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateCustomerProfileRequest() {
		// update a customer profile
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.UPDATE_CUSTOMER_PROFILE);

		customerProfile = CustomerProfile.createCustomerProfile();
		customerProfile.setMerchantCustomerId(customerId);
		customerProfile.setDescription(customerDescription2);
		customerProfile.setEmail(email2);
		customerProfile.setCustomerProfileId(customerProfileId);

		transaction.setCustomerProfile(customerProfile);
		transaction.setCustomerProfileId(customerProfileId);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());

		transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_PROFILE);

		transaction.setCustomerProfileId(customerProfileId);

		result = (Result<Transaction>) merchant.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerProfileId());
		Assert.assertNotNull(result.getCustomerProfile());
		Assert.assertEquals(customerDescription2, result.getCustomerProfile()
				.getDescription());
		Assert.assertEquals(email2, result.getCustomerProfile().getEmail());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateCustomerPaymentProfileRequest() {
		// update customer payment profile
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.UPDATE_CUSTOMER_PAYMENT_PROFILE);
		transaction.setRefId(refId);
		transaction.setCustomerProfileId(customerProfileId);
		String newCompanyName = company + System.currentTimeMillis();
		billingInfo.setCompany(newCompanyName);
		paymentProfileCC.setBillTo(billingInfo);
		paymentProfileCC.setCustomerPaymentProfileId(customerPaymentProfileId);		
		transaction.addPaymentProfile(paymentProfileCC);
		//
		ArrayList<Payment> pl = paymentProfileCC.getPaymentList();
		Payment p  = pl.get(0);
		if(p.getCreditCard()!=null)
			{p.getCreditCard().setExpirationDate(CreditCard.MASKED_EXPIRY_DATE);}
		transaction.setValidationMode(ValidationModeType.TEST_MODE);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertEquals(1, result.getDirectResponseList().size());

		// check if the new data was saved
		transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_PAYMENT_PROFILE);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);

		result = (Result<Transaction>) merchant.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerPaymentProfile());
		Assert.assertEquals(newCompanyName, result.getCustomerPaymentProfile()
				.getBillTo().getCompany());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateCustomerShippingAddressRequest() {
		// update customer shipping address
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.UPDATE_CUSTOMER_SHIPPING_ADDRESS);
		transaction.setRefId(refId);

		String newCompanyName = company + System.currentTimeMillis();
		shippingInfo.setCompany(newCompanyName);
		shippingInfo.setAddressId(customerShippingAddressId);
		customerProfile.addShipToAddress(shippingInfo);
		customerProfile.setCustomerProfileId(customerProfileId);

		transaction.setCustomerProfile(customerProfile);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());

		// check if the new data was saved
		transaction = merchant
				.createCIMTransaction(TransactionType.GET_CUSTOMER_SHIPPING_ADDRESS);
		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);

		result = (Result<Transaction>) merchant.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getCustomerProfile());
		Assert.assertTrue(result.getCustomerProfile().getShipToAddressList()
				.size() > 0);
		Assert.assertEquals(newCompanyName, result.getCustomerProfile()
				.getShipToAddressList().get(0).getCompany());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateSplitTenderGroupRequest() {

		// perform aim call
		try {
			MultiOrderAuth_Capture_Void_CreditTest aimTest = new MultiOrderAuth_Capture_Void_CreditTest();
			aimTest.setUp();
			splitTenderId = aimTest.createSplitTenderAuthCapture();
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// update split tender group
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.UPDATE_SPLIT_TENDER_GROUP);

		PaymentTransaction splitTenderPaymentTransaction = PaymentTransaction
				.createPaymentTransaction();
		splitTenderPaymentTransaction.setSplitTenderId(splitTenderId);
		splitTenderPaymentTransaction
				.setSplitTenderStatus(SplitTenderStatus.VOIDED);

		transaction.setPaymentTransaction(splitTenderPaymentTransaction);
		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testValidateCustomerPaymentProfileRequest() {
		// update split tender group
		net.authorize.cim.Transaction transaction = merchant
				.createCIMTransaction(TransactionType.VALIDATE_CUSTOMER_PAYMENT_PROFILE);

		transaction.setCustomerProfileId(customerProfileId);
		transaction.setCustomerPaymentProfileId(customerPaymentProfileId);
		transaction.setCustomerShippingAddressId(customerShippingAddressId);
		transaction.setCardCode(cardholderAuthenticationValue);
		transaction.setValidationMode(ValidationModeType.TEST_MODE);

		Result<Transaction> result = (Result<Transaction>) merchant
				.postTransaction(transaction);

		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCustomerProfileIssueCdataTagsinNameFields() throws Exception {
		
		final String badString = "Bad & char ";
		
		//create Customer profile
		String badXml = XmlUtility.getXml(customer);
		Customer badCustomer = XmlUtility.create(badXml, Customer.class);
		Address billTo = badCustomer.getBillTo();
		billTo.setCompany( badString  + billTo.getCompany()); //badString  + 

		badXml = XmlUtility.getXml(paymentProfileCC);
		PaymentProfile badPaymentProfile = XmlUtility.create(badXml, PaymentProfile.class);
		badPaymentProfile.setBillTo(billTo);

		badXml = XmlUtility.getXml(customerProfile);
		CustomerProfile badCustomerProfile = XmlUtility.create(badXml, CustomerProfile.class);
		badCustomerProfile.setDescription( badCustomerProfile.getDescription() ); //badString  + 
		
		System.out.println( "BadCustomerProfile: " + XmlUtility.getXml(badCustomerProfile));
		System.out.println( "BadPaymentProfile: " + XmlUtility.getXml(badPaymentProfile));
		
		Result<Transaction> result = createCustomerProfile(badCustomerProfile, badPaymentProfile, ValidationModeType.TEST_MODE);
		
		Assert.assertNotNull(result);
		System.out.println( "Result: " + result.toString());
		result.printMessages();

		if ( null != result.getTransaction()) { System.out.println( "Result: " + result.getTransaction().toXMLString());}
		
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
		Assert.assertNotNull(result.getCustomerProfileId());
		Assert.assertNotNull(result.getCustomerPaymentProfileIdList());
		Assert.assertTrue(result.getCustomerPaymentProfileIdList().size() == 1);
		Assert.assertTrue(result.getDirectResponseList().size() == 1);

		// delete a customer profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.DELETE_CUSTOMER_PROFILE);
		setRefId(transaction);
		transaction.setCustomerProfileId(result.getCustomerProfileId());
		result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);
		result.printMessages();
		Assert.assertTrue(result.isOk());
		Assert.assertNotNull(result.getRefId());
	}

	private static Result<Transaction> createCustomerProfile(
			CustomerProfile customerProfile, 
			PaymentProfile paymentProfile,
			ValidationModeType validationModeType ) {
		
		Assert.assertNotNull(customerProfile);
		Assert.assertNotNull(paymentProfile);
		Assert.assertNotNull(validationModeType);

		// Create a new customer payment profile
		net.authorize.cim.Transaction transaction = merchant.createCIMTransaction(TransactionType.CREATE_CUSTOMER_PROFILE);
		
		System.out.println( "Request: " + transaction.toXMLString());
		
		setRefId(transaction);
		transaction.setCustomerProfile(customerProfile); 
		transaction.addPaymentProfile(paymentProfile); 
		transaction.setValidationMode(validationModeType); 

		@SuppressWarnings("unchecked")
		Result<Transaction> result = (Result<Transaction>) merchant.postTransaction(transaction);
		Assert.assertNotNull(result);

		customerProfileId = result.getCustomerProfileId();
		
		return result;
	}
	
	private static void setRefId( net.authorize.cim.Transaction transaction) {
		transaction.setRefId( "REFID:" + System.currentTimeMillis());
	}
}
