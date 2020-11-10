package com.corsosystems;

import com.corsosystems.db.HubSettingsRecord;
import com.corsosystems.web.HubSettingsTable;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private GatewayContext gatewayContext;
    private HueProvider hueProvider;

    @Override
    public void setup(GatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;

        BundleUtil.get().addBundle("HubSettingsRecord", HubSettingsRecord.class, "HubSettingsRecord");
        BundleUtil.get().addBundle("HubSettingsTable", HubSettingsTable.class, "HubSettingsTable");

        // Ensure the internal db table exists
        try {
            gatewayContext.getSchemaUpdater().updatePersistentRecords(
                    HubSettingsRecord.META);
        } catch (SQLException e) {
            logger.error("Error verifying schema.", e);
        }
    }

    @Override
    public void startup(LicenseState licenseState) {
        hueProvider = new HueProvider(gatewayContext);
    }

    @Override
    public void shutdown() {
        hueProvider.shutDown();

        BundleUtil.get().removeBundle("HubSettingsRecord");
        BundleUtil.get().removeBundle("HubSettingsTable");
    }

    @Override
    public List<ConfigCategory> getConfigCategories() {
        return Collections.singletonList(HubSettingsTable.CONFIG_CATEGORY);
    }

    @Override
    public List<? extends IConfigTab> getConfigPanels() {
        return Arrays.asList(HubSettingsTable.CONFIG_ENTRY);
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }
}
