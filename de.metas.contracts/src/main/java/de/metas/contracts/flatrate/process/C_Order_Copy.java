package de.metas.contracts.flatrate.process;

import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.Adempiere;
import org.compiere.util.Ini;

import de.metas.contracts.order.ContractOrderService;
import de.metas.contracts.subscription.impl.subscriptioncommands.ExtendContractOrder;
import de.metas.contracts.subscription.model.I_C_Order;
import de.metas.order.OrderId;
import de.metas.process.IProcessPrecondition;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessExecutionResult.RecordsToOpen.OpenTarget;
import de.metas.process.ProcessPreconditionsResolution;

public class C_Order_Copy extends JavaProcess implements IProcessPrecondition
{
	@Override
	protected String doIt()
	{
		final I_C_Order existentOrder = getRecord(I_C_Order.class);
		final I_C_Order newOrder = ExtendContractOrder.extend(existentOrder);
		
		final int adWindowId = getProcessInfo().getAD_Window_ID();
		final TableRecordReference ref = TableRecordReference.of(newOrder);
		
		if (adWindowId > 0 && !Ini.isClient())
		{
			getResult().setRecordToOpen(ref, adWindowId, OpenTarget.SingleDocument);
		}
		else
		{
			getResult().setRecordToSelectAfterExecution(ref);
		}
		
		return MSG_OK;
	}


	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(IProcessPreconditionsContext context)
	{
		
		if (!context.isSingleSelection())
		{
			return ProcessPreconditionsResolution.rejectBecauseNotSingleSelection();
		}

		if (!I_C_Order.Table_Name.equals(context.getTableName()))
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("not running on C_Order table");
		}
		
		final ContractOrderService contractOrderRepository = Adempiere.getBean(ContractOrderService.class);
		if (!contractOrderRepository.isContractSalesOrder( OrderId.ofRepoId(context.getSingleSelectedRecordId())))
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("not running on contract order");
		}
		
		return ProcessPreconditionsResolution.accept();
	}

	
}
