package com.corsosystems.web;

import com.corsosystems.db.HubSettingsRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.web.components.RecordActionTable;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import org.apache.commons.lang3.tuple.Pair;

public class HubSettingsTable extends RecordActionTable<HubSettingsRecord> {
    public static final ConfigCategory CONFIG_CATEGORY = new ConfigCategory("Hue", "HubSettingsTable.nav.header", 700);

    public static final IConfigTab CONFIG_ENTRY = DefaultConfigTab.builder()
            .category(CONFIG_CATEGORY)
            .name("hubsettings")
            .i18n("HubSettingsTable.nav.title")
            .page(HubSettingsTable.class)
            .terms("Phillips", "Hue", "Hub")
            .build();

    public HubSettingsTable(IConfigPage configPage) {
        super(configPage);
    }

    @Override
    protected RecordMeta<HubSettingsRecord> getRecordMeta() {
        return HubSettingsRecord.META;
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return CONFIG_ENTRY.getMenuLocation();
    }
}
