/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.coheigea.bigdata.hbase;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;

/**
 * A basic test that launches HBase, creates a table + then queries it
 */
public class HBaseTest {
    
    private static int port;
    private static HBaseTestingUtility utility;
    
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        port = getFreePort();
        
        utility = new HBaseTestingUtility();
        utility.getConfiguration().set("test.hbase.zookeeper.property.clientPort", "" + port);
        utility.getConfiguration().set("hbase.master.port", "" + getFreePort());
        utility.getConfiguration().set("hbase.master.info.port", "" + getFreePort());
        utility.getConfiguration().set("hbase.regionserver.port", "" + getFreePort());
        utility.getConfiguration().set("hbase.regionserver.info.port", "" + getFreePort());
        utility.getConfiguration().set("zookeeper.znode.parent", "/hbase-unsecure");
        utility.startMiniCluster();
        
        // Create a table
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();
        
        // Create a table
        if (!admin.tableExists(TableName.valueOf("temp"))) {
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf("temp"));
    
            // Adding column families to table descriptor
            tableDescriptor.addFamily(new HColumnDescriptor("colfam1"));
            tableDescriptor.addFamily(new HColumnDescriptor("colfam2"));
    
            admin.createTable(tableDescriptor);
        }

        conn.close();
        
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        utility.shutdownMiniCluster();
    }
    
    @org.junit.Test
    public void testHBaseQueryListOfTables() throws Exception {
        
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        // Now query the list of tables
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();

        HTableDescriptor[] tableDescriptors = admin.listTables();
        Assert.assertEquals(1, tableDescriptors.length);

        conn.close();
    }
    
    @org.junit.Test
    public void testHBaseReadWriteDelete() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(TableName.valueOf("temp"));
        
        // Add a new row
        Put put = new Put(Bytes.toBytes("row1"));
        put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val1"));
        
        table.put(put);

        // Read the new row
        Get get = new Get(Bytes.toBytes("row1"));
        Result result = table.get(get);
        byte[] valResult = result.getValue(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"));
        Assert.assertTrue(Arrays.equals(valResult, Bytes.toBytes("val1")));
        
        // Now delete the new row
        Delete delete = new Delete(Bytes.toBytes("row1"));
        table.delete(delete);
        
        conn.close();
    }
    
    private static int getFreePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }
}
