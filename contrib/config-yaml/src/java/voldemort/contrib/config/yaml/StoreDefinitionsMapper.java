package voldemort.contrib.config.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import org.yaml.snakeyaml.Yaml;
import voldemort.client.RoutingTier;
import voldemort.routing.RoutingStrategyType;
import voldemort.serialization.Compression;
import voldemort.serialization.Serializer;
import voldemort.serialization.SerializerDefinition;
import voldemort.store.StoreDefinition;
import voldemort.store.StoreDefinitionBuilder;
import voldemort.store.StoreUtils;
import voldemort.store.slop.strategy.HintedHandoffStrategyType;
import voldemort.store.system.SystemStoreConstants;
import voldemort.store.views.ViewStorageConfiguration;
import voldemort.xml.MappingException;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create a List of StoreDefinition objects from a YAML representation.
 */
public class StoreDefinitionsMapper {

    public List<StoreDefinition> readStoreList(File f) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(f);
            return readStoreList(reader);
        } finally {
            if(reader != null)
                reader.close();
        }
    }

    /**
     * Read list of StoreDefinition information from a reader containing the yml representation
     *
     * @param input the yaml data via a reader instance.
     * @return the list of StoreDefinition
     * @throws RuntimeException if the store configuration is not valid
     */
    public List<StoreDefinition> readStoreList(Reader input) {
        Yaml yaml = new Yaml();
        StoresConfig config = Validations.validate(yaml.loadAs(input, StoresConfig.class));
        return config.build();
    }

    private static class StoresConfig {
        //@Min(value=1, message="at least one store must be defined")
        @Valid
        public List<StoreConfig> stores = new ArrayList<StoreConfig>();

        @Valid
        public List<ViewConfig> views = new ArrayList<ViewConfig>();

        public List<StoreDefinition> build() {
            List<StoreDefinition> definitions = new ArrayList<StoreDefinition>();
            for (StoreConfig sc : stores) {
                definitions.add(sc.build());
            }
            for (ViewConfig vc : views) {
                definitions.add(vc.build(definitions));
            }
            return definitions;
        }
    }

    private static class StoreConfig {
        @NotNull(message="name of store is mandatory")
        public String name;

        @NotNull(message="persistence of store is mandatory")
        public String persistence;

        public String description;

        public List<String> owners = new ArrayList<String>();

        @Min(value=1, message="replicationFactor of store must not be less than 1")
        @Max(value=Integer.MAX_VALUE)
        public int replicationFactor;

        @Valid
        public List<ZoneReplicationFactorConfig> zoneReplicationFactor = new ArrayList<ZoneReplicationFactorConfig>();

        public Integer zoneCountReads;
        public Integer zoneCountWrites;

        @Min(value=1, message="requiredReads of store must not be less than 1")
        @Max(value=Integer.MAX_VALUE)
        public int requiredReads;

        @Min(value=1, message="requiredWrites of store must not be less than 1")
        @Max(value=Integer.MAX_VALUE)
        public int requiredWrites;

        public Integer preferredReads;
        public Integer preferredWrites;

        @NotNull(message="keySerializer of store is mandatory")
        public SerializerConfig keySerializer;

        @NotNull(message="valueSerializer of store is mandatory")
        public SerializerConfig valueSerializer;

        public String routing;

        @NotEmpty(message="routingStrategy of store is mandatory")
        public String routingStrategy = RoutingStrategyType.CONSISTENT_STRATEGY;

        public Integer retentionDays;
        public Integer retentionScanThrottleRate;
        public Integer retentionFrequency;
        public String hintedHandoffStrategy;
        public Integer hintPreflistSize;

        public long memoryFootprint = 0;

        public StoreDefinition build() {
            SerializerDefinition keySerializer = this.keySerializer.build();
            if(keySerializer.getAllSchemaInfoVersions().size() > 1)
                throw new MappingException("Only a single schema is allowed for the store key.");
            SerializerDefinition valueSerializer = this.valueSerializer.build();

            HashMap<Integer, Integer> zoneReplicationFactor = null;
            for (ZoneReplicationFactorConfig zrfc : this.zoneReplicationFactor) {
                if (zoneReplicationFactor == null) {
                    zoneReplicationFactor = new HashMap<Integer, Integer>();
                }
                zoneReplicationFactor.put(zrfc.zoneId, zrfc.replicationFactor);
            }
            if(routingStrategy.compareTo(RoutingStrategyType.ZONE_STRATEGY) == 0
                    && !SystemStoreConstants.isSystemStore(name)) {
                if(zoneCountReads == null || zoneCountWrites == null || zoneReplicationFactor == null) {
                    throw new MappingException("Have not set one of the following correctly for store '"
                            + name
                            + "' - zoneCountReads, zoneCountWrites, zoneReplicationFactor");
                }
            }
            HintedHandoffStrategyType hintedHandoffStrategyType = this.hintedHandoffStrategy == null ? null :
                    HintedHandoffStrategyType.fromDisplay(hintedHandoffStrategy);
            StoreDefinitionBuilder builder = new StoreDefinitionBuilder()
                    .setName(name)
                    .setType(persistence)
                    .setDescription(description)
                    .setOwners(owners)
                    .setReplicationFactor(replicationFactor)
                    .setZoneReplicationFactor(zoneReplicationFactor)
                    .setKeySerializer(keySerializer)
                    .setValueSerializer(valueSerializer)
                    .setZoneCountReads(zoneCountReads)
                    .setZoneCountWrites(zoneCountWrites)
                    .setRequiredReads(requiredReads)
                    .setRequiredWrites(requiredWrites)
                    .setPreferredReads(preferredReads)
                    .setPreferredWrites(preferredWrites)
                    .setRoutingPolicy(RoutingTier.fromDisplay(routing))
                    .setRoutingStrategyType(routingStrategy)
                    .setRetentionScanThrottleRate(retentionScanThrottleRate)
                    .setRetentionFrequencyDays(retentionFrequency)
                    .setRetentionPeriodDays(retentionDays)
                    .setHintedHandoffStrategy(hintedHandoffStrategyType)
                    .setHintPrefListSize(hintPreflistSize)
                    .setMemoryFootprintMB(memoryFootprint);
            return builder.build();
        }
    }

    private static class ZoneReplicationFactorConfig {
        @NotNull(message="zoneId of zoneReplicationFactor is mandatory")
        public Integer zoneId;

        @NotNull(message="replicationFactor of zoneReplicationFactor is mandatory")
        public Integer replicationFactor;
    }

    private static class SerializerConfig {

        @NotNull(message="type of serializer is mandatory")
        public String type;

        @Valid
        public List<SchemaInfoConfig> schemaInfos = new ArrayList<SchemaInfoConfig>();

        public CompressionConfig compression;

        public SerializerDefinition build() {
            boolean hasVersion = true;
            Map<Integer, String> schemaInfosByVersion = new HashMap<Integer, String>();
            for (SchemaInfoConfig sic : schemaInfos) {
                if (sic.version == null) {
                    hasVersion = false;
                }
                int version = sic.version == null ? 0 : sic.version;
                String previous = schemaInfosByVersion.put(version, sic.value);
                if(previous != null)
                    throw new MappingException("Duplicate version " + version
                            + " found in schema info.");
            }
            if(!hasVersion && schemaInfosByVersion.size() > 1)
                throw new IllegalArgumentException("Specified multiple schemas AND version=none, which is not permitted.");
            Compression compression = this.compression == null ? null : this.compression.build();
            return new SerializerDefinition(type, schemaInfosByVersion, hasVersion, compression);
        }
    }

    private static class SchemaInfoConfig {
        @NotNull(message="value of schemaInfo is mandatory")
        public String value;

        public Integer version;

    }

    private static class CompressionConfig {
        @NotNull(message="type of compression is mandatory")
        public String type;

        public String options;

        public Compression build() {
            return new Compression(type, options);
        }
    }

    private static class ViewConfig {
        @NotEmpty(message="name of view is mandatory")
        public String name;

        @NotEmpty(message="viewOf of view is mandatory")
        public String viewOf;

        public String description;

        public List<String> owners = new ArrayList<String>();

        public String viewClass;
        public Integer zoneCountReads;
        public Integer zoneCountWrites;

        @Min(value=1, message="requiredReads of view must not be less than 1")
        @Max(value=Integer.MAX_VALUE)
        @NotNull(message="requiredReads of view is mandatory")
        public Integer requiredReads;

        @Min(value=1, message="requiredWrites of view must not be less than 1")
        @Max(value=Integer.MAX_VALUE)
        @NotNull(message="requiredWrites of view is mandatory")
        public Integer requiredWrites;

        public Integer preferredReads;
        public Integer preferredWrites;
        public String viewSerializerFactory;
        public SerializerConfig valueSerializer;
        public String routing;
        public String routingStrategy;
        public SerializerConfig transformsSerializer;

        public StoreDefinition build(List<StoreDefinition> stores) {
            StoreDefinition target = StoreUtils.getStoreDef(stores, viewOf);
            if(target == null)
                throw new MappingException("View \"" + name + "\" has target store \"" + viewOf
                        + "\" but no such store exists");
            SerializerDefinition keySerializer = target.getKeySerializer();
            SerializerDefinition valueSerializer = target.getValueSerializer();
            if (this.valueSerializer != null) {
                valueSerializer = this.valueSerializer.build();
            }
            SerializerDefinition transformSerializer = target.getTransformsSerializer();
            if (this.transformsSerializer != null) {
                transformSerializer = this.transformsSerializer.build();
            }
            RoutingTier routingTier = target.getRoutingPolicy();
            if (this.routing != null) {
                routingTier = RoutingTier.fromDisplay(this.routing);
            }
            return new StoreDefinitionBuilder().setName(name)
                    .setViewOf(viewOf)
                    .setType(ViewStorageConfiguration.TYPE_NAME)
                    .setDescription(description)
                    .setOwners(owners)
                    .setRoutingPolicy(routingTier)
                    .setRoutingStrategyType(target.getRoutingStrategyType())
                    .setKeySerializer(keySerializer)
                    .setValueSerializer(valueSerializer)
                    .setTransformsSerializer(transformSerializer)
                    .setReplicationFactor(target.getReplicationFactor())
                    .setZoneReplicationFactor(target.getZoneReplicationFactor())
                    .setPreferredReads(preferredReads)
                    .setRequiredReads(requiredReads)
                    .setPreferredWrites(preferredWrites)
                    .setRequiredWrites(requiredWrites)
                    .setZoneCountReads(zoneCountReads)
                    .setZoneCountWrites(zoneCountWrites)
                    .setView(viewClass)
                    .setSerializerFactory(viewSerializerFactory)
                    .build();
        }

    }

}
