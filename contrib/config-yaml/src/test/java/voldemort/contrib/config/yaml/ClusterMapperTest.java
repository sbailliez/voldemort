package voldemort.contrib.config.yaml;

import junit.framework.Assert;
import org.junit.Test;
import voldemort.cluster.Cluster;

import javax.validation.ConstraintViolationException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @see ClusterMapper
 */
public class ClusterMapperTest {

    @Test
    public void readCluster() throws Exception {
        assertEqualsCluster("cluster-2.xml", "cluster-2.yml");
    }

    @Test
    public void readInvalidClusterWithEmptyName() {
        try {
            readCluster("cluster-invalid-1.yml");
            Assert.fail("Expected violation");
        } catch (ConstraintViolationException e) {
            Assert.assertEquals("name of cluster is mandatory", e.getConstraintViolations().iterator().next().getMessage());
        }
    }

    @Test
    public void readInvalidClusterWithMissingZoneId() {
        try {
            readCluster("cluster-invalid-2.yml");
            Assert.fail("Expected violation");
        } catch (ConstraintViolationException e) {
            Assert.assertEquals("id of zone is mandatory", e.getConstraintViolations().iterator().next().getMessage());
        }
    }

    @Test
    public void readInvalidClusterWithInvalidPartition() {
        try {
            readCluster("cluster-invalid-3.yml");
            Assert.fail("Expected violation");
        } catch (ConstraintViolationException e) {
            Assert.assertEquals("partition id must not be greater than 65535", e.getConstraintViolations().iterator().next().getMessage());
        }
    }

    protected Cluster readCluster(String ymlResource) {
        InputStream ys = getClass().getResourceAsStream(ymlResource);
        return new ClusterMapper().readCluster(new InputStreamReader(ys));
    }

    protected void assertEqualsCluster(String xmlResource, String ymlResource) throws Exception {
        InputStream xs = getClass().getResourceAsStream(xmlResource);
        InputStream ys = getClass().getResourceAsStream(ymlResource);
        Cluster yc = new ClusterMapper().readCluster(new InputStreamReader(ys));
        Cluster xc = new voldemort.xml.ClusterMapper().readCluster(new InputStreamReader(xs));
        Assert.assertEquals(xc, yc);
    }
}