package com.yd.zkReg;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;

/**
 * Created by liuhailin on 2017/1/10.
 */
public abstract class ZKManager implements IZKRegister {

    protected CuratorFramework client;

    protected RetryPolicy retryPolicy;

    /**
     * 参数说明: 1:connectString,是你zk的地址,多个地址中间以逗号隔开,zk在初始化的时候会打乱其顺序。
     * 2:sessionTimeoutMS:一个Session会话的超时时间。 3:connectionTimeoutMs:一个连接创建的超时时间。
     * 4:retryPolicy:重试机制,zk提供了几种默认的重试机制,在包org.apache.curator.retry下面。
     *
     * @param config
     */
    private void newManger(ZKConifg config) {
        // retryPolicy = new RetryNTimes(Integer.MAX_VALUE, 1000);
        retryPolicy = new ExponentialBackoffRetry(1000, 3);

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(config.getServers())
                .retryPolicy(retryPolicy).sessionTimeoutMs(config.getSessionTimeOut())
                .connectionTimeoutMs(config.getConnectionTimeOut());

        if (config.getNameSpace() != null && !"".equals(config.getNameSpace())) {
            builder.namespace(config.getNameSpace());
        }
        // builder.aclProvider(new ACLProvider() {
        //
        // private List<ACL> acl = Lists.newArrayList();
        //
        // @Override
        // public List<ACL> getDefaultAcl() {
        // acl.add(new ACL(ZooDefs.Perms.ALL, new Id("ip",
        // Application.getIpAddress())));
        // return acl;
        // }
        //
        // @Override
        // public List<ACL> getAclForPath(String s) {
        // return acl;
        // }
        // });
        client = builder.build();
    }

    public ZKManager(ZKConifg config, boolean isTest) {

        if (!isTest) {
            newManger(config);
        } else {
            TestingServer server = null;
            try {
                server = new TestingServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
            retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy);
        }
    }

    public void init() {
        client.start();
        try {
            client.blockUntilConnected();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        // client.close();
        CloseableUtils.closeQuietly(client);
    }

}
