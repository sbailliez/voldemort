package voldemort.contrib.config.yaml;

import junit.framework.Assert;
import org.junit.Test;
import voldemort.store.StoreDefinition;

import javax.validation.ConstraintViolationException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @see StoreDefinitionsMapper
 */
public class StoreDefinitionsMapperTest {
    @Test
    public void readStores() throws Exception {
        assertEqualsStores("store-1.xml", "store-1.yml");
    }

    @Test
    public void readCompressedStore() throws Exception {
        assertEqualsStores("compressed-store.xml", "compressed-store.yml");
    }

    @Test
    public void readInvalidStoreWithEmptyName() {
        try {
            readStores("store-invalid-1.yml");
            Assert.fail("Expected violation");
        } catch (ConstraintViolationException e) {
            Assert.assertEquals("name of store is mandatory", e.getConstraintViolations().iterator().next().getMessage());
        }
    }

    @Test
    public void readInvalidStoreWithInvalidRequiredReads() {
        try {
            readStores("store-invalid-2.yml");
            Assert.fail("Expected violation");
        } catch (ConstraintViolationException e) {
            Assert.assertEquals("requiredReads of store must not be less than 1", e.getConstraintViolations().iterator().next().getMessage());
        }
    }

    @Test
    public void readInvalidClusterWithInvalidPartition() {
        try {
            readStores("store-invalid-3.yml");
            Assert.fail("Expected violation");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Sum total of zones (3) does not match the total replication factor (5)", e.getMessage());
        }
    }

    protected List<StoreDefinition> readStores(String ymlResource) {
        InputStream ys = getClass().getResourceAsStream(ymlResource);
        return new StoreDefinitionsMapper().readStoreList(new InputStreamReader(ys));
    }

    protected void assertEqualsStores(String xmlResource, String ymlResource) throws Exception {
        InputStream xs = getClass().getResourceAsStream(xmlResource);
        InputStream ys = getClass().getResourceAsStream(ymlResource);
        List<StoreDefinition> yc = new StoreDefinitionsMapper().readStoreList(new InputStreamReader(ys));
        List<StoreDefinition> xc = new voldemort.xml.StoreDefinitionsMapper().readStoreList(new InputStreamReader(xs));
        Assert.assertEquals(xc, yc);
    }
}
