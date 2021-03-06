package org.openmrs.module.openhmis.inventory.api.impl;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.inventory.api.WellKnownOperationTypes;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.model.ItemStock;
import org.openmrs.module.openhmis.inventory.api.model.ItemStockDetail;
import org.openmrs.module.openhmis.inventory.api.model.ReservedTransaction;
import org.openmrs.module.openhmis.inventory.api.model.StockOperation;
import org.openmrs.module.openhmis.inventory.api.model.StockOperationStatus;
import org.openmrs.module.openhmis.inventory.api.model.Stockroom;

public class DistributionOperationTest extends BaseOperationTest {
	@Test
	public void submitOperation_shouldReduceBatchQuantity_singleBatch_noExpirationDate() throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 6);

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());
		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(4, stock.getQuantity());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(4), detailAfterSubmit.getQuantity());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(1, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceBatchQuantity_singleBatch_specificExpirationDate() throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString = "31-08-2016";
		Date expirationDate = sdf.parse(dateInString);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));
		detail.setExpiration(expirationDate);

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 6, expirationDate);

		detail = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());
		Assert.assertEquals(expirationDate, detail.getExpiration());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(4, stock.getQuantity());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(4), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detailAfterSubmit.getBatchOperation());
		Assert.assertEquals(expirationDate, detailAfterSubmit.getExpiration());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(1, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceNullBatchQuantityFurtherIfAdjustmentQuantityNegative_singleBatch_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(-10);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(-10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(null);
		detail.setExpiration(null);

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 6);

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(-10), detail.getQuantity());
		Assert.assertNull(detail.getBatchOperation());
		Assert.assertNull(detail.getExpiration());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(-16, stock.getQuantity());
		Assert.assertEquals(1, stock.getDetails().size());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(-16), detailAfterSubmit.getQuantity());
		Assert.assertNull(detail.getBatchOperation());
		Assert.assertNull(detail.getBatchOperation());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(1, operation.getReserved().size());
	}

	@Test(expected = APIException.class)
	public void submitOperation_shouldThrowExceptionIfDistributionQuantityIsNegative_singleBatch_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setBatchOperation(operationDataService.getById(1));
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, -6);

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
	}

	@Test
	public void submitOperation_shouldRemoveBatchWithZeroQuantity_singleBatch_noExpirationDate() throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 10);

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNull(stock);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(1, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldRemoveBatchWithZeroQuantity_singleBatch_specificExpirationDate() throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString = "31-08-2016";
		Date expirationDate = sdf.parse(dateInString);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));
		detail.setExpiration(expirationDate);

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 10, expirationDate);

		detail = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNull(stock);
		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertNull(detailAfterSubmit);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(1, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceFromNextBatchIfQuantityOfOneBatchIsNotEnough_multipleBatches_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 25);

		Collection<ItemStockDetail> findDetails = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(findDetails);
		Assert.assertEquals(2, findDetails.size());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());
		stock = stockroomDataService.getItem(sourceRoom, item);
		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertEquals(new Integer(5), detailAfterSubmit.getQuantity());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceFromNextBatchIfQuantityOfOneBatchIsNotEnough_multipleBatches_specificExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString = "31-08-2016";
		Date expirationDate = sdf.parse(dateInString);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));
		detail1.setExpiration(expirationDate);

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));
		detail2.setExpiration(expirationDate);

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 25, expirationDate);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, expirationDate);
		Assert.assertNotNull(details);
		Assert.assertEquals(2, details.size());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(5, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());
		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertEquals(new Integer(5), detailAfterSubmit.getQuantity());
		Assert.assertEquals(expirationDate, detailAfterSubmit.getExpiration());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldRemoveMultipleBatchesIfBatchQuantityIsZero_multipleBatches_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 30);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(details);
		Assert.assertEquals(2, details.size());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNull(stock);

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNull(detailsAfterSubmit);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldRemoveMultipleBatchesIfBatchQuantityIsZero_multipleBatches_specificExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString = "31-08-2016";
		Date expirationDate = sdf.parse(dateInString);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));
		detail1.setExpiration(expirationDate);

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));
		detail2.setExpiration(expirationDate);

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 30, expirationDate);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, expirationDate);
		Assert.assertNotNull(details);
		Assert.assertEquals(2, details.size());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNull(stock);

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNull(detailsAfterSubmit);
		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate);
		Assert.assertNull(detailsAfterSubmit);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceBatchQuantityAndCreateNullBatchIfTooMuchIsDeducted_singleBatch_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 60);

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());
		Assert.assertNull(detail.getExpiration());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(-50, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(-50), detailAfterSubmit.getQuantity());
		Assert.assertNull(detailAfterSubmit.getBatchOperation());
		Assert.assertNull(detailAfterSubmit.getExpiration());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldReduceBatchQuantityAndCreateNullBatchIfTooMuchIsDeducted_singleBatch_specificExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString = "31-08-2016";
		Date expirationDate = sdf.parse(dateInString);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));
		detail.setExpiration(expirationDate);

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 60, expirationDate);

		detail = stockOperationServiceImplTest.findDetail(stock, expirationDate);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(10), detail.getQuantity());
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());
		Assert.assertEquals(expirationDate, detail.getExpiration());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertEquals(-50, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate);
		Assert.assertNull(detailsAfterSubmit);

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(-50), detailAfterSubmit.getQuantity());
		Assert.assertNull(detailAfterSubmit.getBatchOperation());
		Assert.assertNull(detailAfterSubmit.getExpiration());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldCreateNullBatchIfTooMuchIsDeductedFromASpecificBatchAndLeaveTheOterBatchUnchanged_multipleBatches_specificExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(35);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString1 = "31-08-2016";
		Date expirationDate1 = sdf.parse(dateInString1);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(15);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));
		detail1.setExpiration(expirationDate1);

		//specify different expiration date
		String dateInString2 = "31-09-2016";
		Date expirationDate2 = sdf.parse(dateInString2);

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));
		detail2.setExpiration(expirationDate2);

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 30, expirationDate1);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		details = stockOperationServiceImplTest.findDetails(stock, expirationDate2);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		details = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNull(details);

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNotNull(stock);
		Assert.assertEquals(5, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate2);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		ItemStockDetail detail = stockOperationServiceImplTest.findDetail(stock, expirationDate2);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(20), detail.getQuantity());

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(-15), detail.getQuantity());

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNull(detailsAfterSubmit);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldCreateNullBatchIfTooMuchIsDeductedAndStockQuantityResultZero_multipleBatches_noExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString1 = "31-08-2016";
		Date expirationDate1 = sdf.parse(dateInString1);

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));
		detail2.setExpiration(expirationDate1);

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 30);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		details = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		ItemStockDetail detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(operationDataService.getById(2), detail.getBatchOperation());

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNotNull(stock);
		Assert.assertEquals(0, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		ItemStockDetail detailAfterSubmit = stockOperationServiceImplTest.findDetail(stock, expirationDate1);
		Assert.assertNotNull(detailAfterSubmit);
		Assert.assertEquals(new Integer(20), detailAfterSubmit.getQuantity());

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertNull(detail.getBatchOperation());
		Assert.assertNull(detail.getExpiration());
		Assert.assertEquals(new Integer(-20), detail.getQuantity());

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void submitOperation_shouldCreateNullBatchIfTooMuchIsDeductedAndStockQuantityResultZero_multipleBatches_specificExpirationDate()
	        throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(30);

		//specify expiration date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
		String dateInString1 = "31-08-2016";
		Date expirationDate1 = sdf.parse(dateInString1);

		// Add some item stock with different qualifiers to the source room
		ItemStockDetail detail1 = new ItemStockDetail();
		detail1.setItem(item);
		detail1.setStockroom(sourceRoom);
		detail1.setQuantity(10);
		detail1.setCalculatedBatch(false);
		detail1.setCalculatedExpiration(false);
		detail1.setBatchOperation(operationDataService.getById(2));
		detail1.setExpiration(expirationDate1);

		//specify different expiration date
		String dateInString2 = "31-09-2016";
		Date expirationDate2 = sdf.parse(dateInString2);

		ItemStockDetail detail2 = new ItemStockDetail();
		detail2.setItem(item);
		detail2.setStockroom(sourceRoom);
		detail2.setQuantity(20);
		detail2.setCalculatedBatch(false);
		detail2.setCalculatedExpiration(false);
		detail2.setBatchOperation(operationDataService.getById(1));
		detail2.setExpiration(expirationDate2);

		stock.addDetail(detail1);
		stock.addDetail(detail2);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.NEW);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A122");
		operation.setOperationDate(new Date());
		operation.addItem(item, 30, expirationDate1);

		Collection<ItemStockDetail> details = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		details = stockOperationServiceImplTest.findDetails(stock, expirationDate2);
		Assert.assertNotNull(details);
		Assert.assertEquals(1, details.size());

		details = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNull(details);

		Assert.assertEquals(StockOperationStatus.NEW, operation.getStatus());

		service.submitOperation(operation);
		Context.flushSession();

		stock = stockroomDataService.getItem(sourceRoom, item);
		Assert.assertNotNull(stock);
		Assert.assertEquals(0, stock.getQuantity());

		Collection<ItemStockDetail> detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate2);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, null);
		Assert.assertNotNull(detailsAfterSubmit);
		Assert.assertEquals(1, detailsAfterSubmit.size());

		ItemStockDetail detail = stockOperationServiceImplTest.findDetail(stock, expirationDate2);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(20), detail.getQuantity());

		detail = stockOperationServiceImplTest.findDetail(stock, null);
		Assert.assertNotNull(detail);
		Assert.assertEquals(new Integer(-20), detail.getQuantity());

		detailsAfterSubmit = stockOperationServiceImplTest.findDetails(stock, expirationDate1);
		Assert.assertNull(detailsAfterSubmit);

		Assert.assertEquals(StockOperationStatus.PENDING, operation.getStatus());
		Assert.assertEquals(2, operation.getReserved().size());
	}

	@Test
	public void calculateReservations_shouldCreateAnotherTransactionIfOneIsNotEnoughTo() throws Exception {
		Stockroom sourceRoom = stockroomDataService.getById(0);

		Item item = itemTest.createEntity(true);
		itemDataService.save(item);

		ItemStock stock = new ItemStock();
		stock.setItem(item);

		stock.setStockroom(sourceRoom);
		stock.setQuantity(10);

		ItemStockDetail detail = new ItemStockDetail();
		detail.setItem(item);
		detail.setStockroom(sourceRoom);
		detail.setQuantity(10);
		detail.setCalculatedBatch(false);
		detail.setCalculatedExpiration(false);
		detail.setBatchOperation(operationDataService.getById(2));

		stock.addDetail(detail);

		itemStockDataService.save(stock);
		Context.flushSession();

		// Create the stock operation
		StockOperation operation = new StockOperation();
		operation.setInstanceType(WellKnownOperationTypes.getDistribution());
		operation.setStatus(StockOperationStatus.PENDING);
		operation.setSource(sourceRoom);
		operation.setOperationNumber("A123");
		operation.setOperationDate(new Date());
		operation.addItem(item, 20);
		final ReservedTransaction tx = operation.addReserved(item, 20);
		tx.setCalculatedBatch(true);
		tx.setCalculatedExpiration(true);

		Assert.assertEquals(1, operation.getReserved().size());

		service.calculateReservations(operation);

		Assert.assertEquals(2, operation.getReserved().size());
	}

}
