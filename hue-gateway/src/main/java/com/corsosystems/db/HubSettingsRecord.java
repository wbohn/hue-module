package com.corsosystems.db;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

public class HubSettingsRecord extends PersistentRecord {

    public static final RecordMeta<HubSettingsRecord> META;

    public static final IdentityField Id;

    public static final StringField HubName;
    public static final StringField IpAddress;
    public static final StringField ApiKey;
    public static final Category HubCategory;

    static {
        META = new RecordMeta<>(
                HubSettingsRecord.class, "HueSettings").setNounKey("HubSettingsRecord.Noun").setNounPluralKey(
                "HubSettingsRecord.Noun.Plural");

        Id = new IdentityField(META, "HueSettings_id");

        // Hub Category
        HubName = new StringField(META, "HubName", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
        HubName.setUnique(true);

        IpAddress = new StringField(META, "IpAddress", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
        IpAddress.setUnique(true);

        ApiKey = new StringField(META, "ApiKey", SFieldFlags.SMANDATORY);
        ApiKey.setUnique(true);

        HubCategory = new Category("HubSettingsRecord.Category.Hub", 100)
                .include(HubName, IpAddress, ApiKey);
    }

    public String getHubName() {
        return getString(HubName);
    }

    public String getIpAddress() {
        return getString(IpAddress);
    }

    public String getApiKey() {
        return getString(ApiKey);
    }

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
}
