package de.metas.procurement.base.rfq;

import java.util.List;

import org.adempiere.util.Services;

import de.metas.procurement.base.IPMM_RfQ_BL;
import de.metas.procurement.base.IWebuiPush;
import de.metas.procurement.base.impl.SyncObjectsFactory;
import de.metas.procurement.sync.protocol.SyncRfQ;
import de.metas.rfq.IRfQResponsePublisher;
import de.metas.rfq.exceptions.RfQPublishException;
import de.metas.rfq.model.I_C_RfQResponse;

/*
 * #%L
 * de.metas.procurement.base
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Publishes {@link I_C_RfQResponse} to procurement webui server.
 *
 * @author metas-dev <dev@metas-fresh.com>
 *
 */
public class PMMWebuiRfQResponsePublisher implements IRfQResponsePublisher
{
	public static final transient PMMWebuiRfQResponsePublisher instance = new PMMWebuiRfQResponsePublisher();

	private PMMWebuiRfQResponsePublisher()
	{
		super();
	}
	
	@Override
	public String getDisplayName()
	{
		return "procurement WebUI";
	}

	@Override
	public void publish(final I_C_RfQResponse rfqResponse)
	{
		try
		{
			publish0(rfqResponse);
		}
		catch (Exception e)
		{
			throw RfQPublishException.wrapIfNeeded(e)
					.setC_RfQResponse(rfqResponse);
		}
	}

	private void publish0(final I_C_RfQResponse rfqResponse)
	{
		if (!Services.get(IPMM_RfQ_BL.class).isProcurement(rfqResponse))
		{
			return;
		}

		final List<SyncRfQ> syncRfqs = SyncObjectsFactory.newFactory()
				.createSyncRfQs(rfqResponse);
		if (syncRfqs.isEmpty())
		{
			return;
		}

		Services.get(IWebuiPush.class).pushRfQs(syncRfqs);
	}
}
