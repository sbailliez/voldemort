package voldemort.contrib.config.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import org.yaml.snakeyaml.Yaml;
import voldemort.cluster.Cluster;
import voldemort.cluster.Node;
import voldemort.cluster.Zone;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Create a Cluster object from a YAML representation.
 */
public class ClusterMapper {

    public static final Integer MAX_PARTITIONID = 65535;

    public Cluster readCluster(File f) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(f);
            return readCluster(reader);
        } finally {
            if(reader != null)
                reader.close();
        }
    }

    /**
     * Read cluster information from a reader containing the yml representation
     *
     * @param input the yaml data via a reader instance.
     * @return the cluster
     * @throws RuntimeException if the cluster configuration is not valid
     */
    public Cluster readCluster(Reader input) throws RuntimeException {
        Yaml yaml = new Yaml();
        return Validations.validate(yaml.loadAs(input, ClusterConfig.class)).build();
    }


    // cluster configuration
    private static class ClusterConfig {
        @NotEmpty(message="name of cluster is mandatory")
        public String name;

        @Valid
        public List<ZoneConfig> zones = new ArrayList<ZoneConfig>();

        @NotEmpty(message="cluster must have at least one server defined")
        @Valid
        public List<ServerConfig> servers = new ArrayList<ServerConfig>();

        public Cluster build() {
            List<Zone> zones = new ArrayList<Zone>();
            for (ZoneConfig zc : this.zones) {
                zones.add(zc.build());
            }
            List<Node> nodes = new ArrayList<Node>();
            for (ServerConfig nc : this.servers) {
                nodes.add(nc.build());
            }
            return new Cluster(name, nodes, zones);
        }
    }

    // zone configuration
    private static class ZoneConfig {
        @NotNull(message="id of zone is mandatory")
        public Integer id;

        @Valid
        public List<Integer> proximityList = new ArrayList<Integer>();

        public Zone build() {
            return new Zone(id, new LinkedList<Integer>(proximityList));
        }
    }

    // server configuration
    private static class ServerConfig {
        @NotNull(message = "id of server is mandatory")
        public Integer id;

        @NotEmpty(message = "host of server is mandatory")
        public String host;

        @NotNull(message = "httpPort of server is mandatory")
        public Integer httpPort;

        @NotNull(message = "socketPort of server is mandatory")
        public Integer socketPort;

        @NotNull(message = "adminPort of server is mandatory")
        public Integer adminPort = -1;

        @Valid
        public List<Integer> partitions = new ArrayList<Integer>();

        @NotNull(message = "zoneId of server is mandatory")
        public Integer zoneId = Zone.DEFAULT_ZONE_ID;


        @Validations.Method(message="partition id must not be greater than 65535")
        public boolean isHavingPartitionsGreaterThanMax() {
            for (int partition : partitions) {
                if (partition > MAX_PARTITIONID) {
                    return false;
                }
            }
            return true;
        }

        public Node build() {
            return new Node(id, host, httpPort, socketPort, adminPort, zoneId, partitions);
        }
    }
}
