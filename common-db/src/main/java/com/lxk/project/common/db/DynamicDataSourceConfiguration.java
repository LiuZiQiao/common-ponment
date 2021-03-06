package com.lxk.project.common.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @Author macos??lxk
 * @create 2021/5/3 ??????1:28
 */

@Configuration
@AutoConfigureBefore
@ConfigurationProperties("spring")
@EnableJpaRepositories
@Import(MapperScannerRegister.class)
public class DynamicDataSourceConfiguration implements InitializingBean, ApplicationContextAware, BeanPostProcessor {
    static Logger logger = LoggerFactory.getLogger(DynamicDataSourceConfiguration.class);
    public static Map<String, Map<String, String>> datasource = new HashMap<>();
    private static ApplicationContext applicationContext;
    /**
     * ???????????????????????????????????????????????????
     */
    private static Map<String, String> defaultDataSourcePropMap = new HashMap<>(9);

    @Value("${spring.jpa.show-sql:true}")
    public Boolean isShowSql;
    @Value("${main.class.package:com.lxk.project}")
    public String defaultMainClassPackage;
    @Value("${e6.datasource.use.hikari:false}")
    private Boolean isUseHikari;
    @Value("#{'${spring.primaryDataSource:spring.datasource.primary}'.substring(18)}")
    private String primaryDataSource;

    public static String mainClassPackage;

    /**
     * ????????????
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        setMainClassPackage();
        //accept???????????????sqlserver??????????????????
        //????????????????????? jpa ??? mybaitis
        mybatisConsumerMap.entrySet().forEach(entity -> entity.getValue().accept(entity.getKey()));
        jpaConsumerMap.entrySet().forEach(entity -> entity.getValue().accept(entity.getKey()));
        if (datasource.size() == 0) {
            throw new RuntimeException("????????????????????????");
        }
        Map<String, String> map = datasource.get(primaryDataSource);
        registerAndGetThisDataSource(primaryDataSource, map);
        //??????????????????
        for (Map.Entry<String, Map<String, String>> entry : datasource.entrySet()) {
            String dataSourceName = entry.getKey();
            Map<String, String> prop = entry.getValue();
            /**
             * ?????????????????????
             */
            DataSource dataSource = registerAndGetThisDataSource(dataSourceName, prop);
            if (dataSource == null) {
                throw new RuntimeException("springApplicationContext????????????" + dataSourceName + "DataSource ??????????????????????????????????????????");
            }
            /**
             * ??????jpa ??????
             */
            jpaConfigBuild(dataSource, dataSourceName, prop);

            //jpa??????   mybatis??????
            /**
             * ??????mybatis??????
             */
            mybatisConfigBuild(dataSource, dataSourceName, prop);
            //???????????? ???????????????????????????
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param environment
     * @return
     */
    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource(Environment environment) {
        logger.info("begin to create primaryDataSource primaryDsNamePrefix = {}" + primaryDataSource);
        /*HikariDataSource dataSource= new HikariDataSource();
        String url = environment.getProperty(primaryDsName + ".jdbcUrl");
        if(StringUtils.isEmpty(url)){
            url = environment.getProperty(primaryDsName + ".url");
        }
        String username = environment.getProperty(primaryDsName + ".username");
        String password = environment.getProperty(primaryDsName + ".password");
        String rsaPassword = environment.getProperty(primaryDsName + ".rsaPassword");
        String driver = environment.getProperty(primaryDsName + ".driverClassName");
        if(StringUtils.isAnyEmpty(url,username)){
            throw new RuntimeException("primary?????????url???username???driver???????????????");
        }
        if(StringUtils.isEmpty(driver)){
            if(url.contains("sqlserver")){
                driver = Constant.SQL_SERVER_DRIVER_CLASS_NAME;
            }else {
                driver = "com.mysql.jdbc.Driver";
            }
        }
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        if(StringUtils.isNotEmpty(password)) {
            dataSource.setPassword(password);
        }else{
            if(StringUtils.isEmpty(rsaPassword)){
                throw new RuntimeException("??????????????????????????????,password???rsa-password??????????????????");
            }
            try {
                dataSource.setPassword(RSAUtil.decode(rsaPassword));
            } catch (Exception e) {
                throw new RuntimeException("???????????????rsa-password????????????");
            }
        }
        dataSource.setDriverClassName(driver);
        logger.info("begin to create completed");*/
        return (DataSource) applicationContext.getBean(primaryDataSource + "DataSource");
    }

    /**
     * ???????????????????????? ??????
     */
    private void setMainClassPackage() {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        if (MapUtils.isEmpty(beansWithAnnotation)) {
            mainClassPackage = defaultMainClassPackage;
            return;
        }
        String classPackage = beansWithAnnotation.values().toArray()[0].getClass().getName();
        if (StringUtils.isEmpty(classPackage)) {
            mainClassPackage = defaultMainClassPackage;
        } else {
            if (classPackage.lastIndexOf(".") < 0) {
                mainClassPackage = defaultMainClassPackage;
            } else {
                mainClassPackage = classPackage.substring(0, classPackage.lastIndexOf("."));
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param dataSourceName
     * @param prop
     * @return
     */
    private DataSource registerAndGetThisDataSource(String dataSourceName, Map<String, String> prop) {
        DataSource dataSource = null;
        try {
            //???????????????
            registerDataSources(dataSourceName + Constant.DATASOURCE, prop);
            //????????????????????????
            dataSource = getBean(dataSourceName + Constant.DATASOURCE);
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException("??????" + dataSourceName + "???????????????????????????application??????????????????????????????????????????????????????");
        }
        logger.info("current DataSoruceName = {}", dataSourceName + "DataSource");
        return dataSource;
    }

    /**
     * jpa ????????????
     *
     * @param dataSource
     * @param dataSourceName
     * @param prop
     */
    private void jpaConfigBuild(DataSource dataSource, String dataSourceName, Map<String, String> prop) {
        if (Objects.equals(prop.get(Constant.JPA_USE), Boolean.FALSE.toString())) {
            logger.info("????????? {} ???????????? JPA", dataSourceName);
            return;
        }
        setDefaultJPAScanAndPackage(dataSourceName, prop);
        logger.info("Jpa {} properties begin ", dataSourceName);
        //??????jpaProperties
        JpaProperties jpaProperties = getBeanForMap(JpaProperties.class);
        jpaProperties.setShowSql(isShowSql);
        logger.info("jpaProperties = {}", jpaProperties);
        //set  entityManagerFactoryPrimary4??????
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(dataSource);
        EntityManagerFactoryBuilder beanForMap = null;
        try {
            beanForMap = getBeanForMap(EntityManagerFactoryBuilder.class);
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException("????????????????????????primaryDataSource??????primary???????????????????????????primaryDataSource??????????????????????????????datasource");
        }
        logger.info("EntityManagerFactoryBuilder = {}", beanForMap);
        constructorArgumentValues.addGenericArgumentValue(beanForMap);
        constructorArgumentValues.addGenericArgumentValue(jpaProperties);
        constructorArgumentValues.addGenericArgumentValue(dataSourceName);
        constructorArgumentValues.addGenericArgumentValue(prop.get(Constant.JPA_MODEL_PACKAGE).split(","));
        logger.info("jpaModelPackage = {}", prop.get(Constant.JPA_MODEL_PACKAGE));
        //??????????????????init?????????????????????????????????  ???????????????????????????beanName?????????????????? ??????return
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = null;
        try {
            setCosAndInitBean(Constant.ENTITY_MANAGER_FACTORY_PRIMARY4 + dataSourceName, EntityManagerFactoryBean.class, constructorArgumentValues);
            //????????????set???????????????  ?????????????????????????????????&
            localContainerEntityManagerFactoryBean = getBean("&" + Constant.ENTITY_MANAGER_FACTORY_PRIMARY4 + dataSourceName);
            logger.info("localContainerEntityManagerFactoryBean = {}", localContainerEntityManagerFactoryBean);
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException(Constant.ENTITY_MANAGER_FACTORY_PRIMARY4 + dataSourceName + "????????????????????????????????????????????????");
        }
        try {
            // ?????? transactionManagerPrimary4
            ConstructorArgumentValues cxf = new ConstructorArgumentValues();
            cxf.addGenericArgumentValue(localContainerEntityManagerFactoryBean.getObject());

            setCosBean(Constant.TRANSACTION_MANAGER_PRIMARY4 + dataSourceName, JpaTransactionManager.class, dataSourceName, cxf);
            // ??????????????? ????????????????????????spring ?????? ioc??? transactionManager??????bean ????????????bean ????????????????????????????????????jpa???????????????
            // ????????????????????? ?????????spring???????????????transactionManager  ?????????????????????????????????????????????????????????????????????????????????primary??????????????????transactionManager
            // ??????setCosBean?????????jpa??????????????????????????????primary
            if (primaryDataSource.contains(dataSourceName)) {
                removeBean(Constant.SPRING_JPA_TRANSACTIONMANAGER_BEAN_NAME);
                setCosBean(Constant.SPRING_JPA_TRANSACTIONMANAGER_BEAN_NAME, JpaTransactionManager.class, dataSourceName, cxf);
            }
            logger.info("this dataSource jpa is build completed");
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException(Constant.TRANSACTION_MANAGER_PRIMARY4 + dataSourceName + "????????????????????????????????????????????????");
        }
    }

    public static void setDefaultJPAScanAndPackage(String dataSourceName, Map<String, String> prop) {
        serDefaultDriver(dataSourceName, prop);
        if (StringUtils.isEmpty(prop.get(Constant.JPA_MODEL_PACKAGE))) {
            logger.info("????????? {}????????? {} ,??????????????????", dataSourceName, Constant.JPA_MODEL_PACKAGE);
            prop.put(Constant.JPA_MODEL_PACKAGE, mainClassPackage + ".**.po");
        }
        if (StringUtils.isEmpty(prop.get(Constant.JPA_SCAN))) {
            logger.info("????????? {}????????? {} ,??????????????????", dataSourceName, Constant.JPA_SCAN);
            prop.put(Constant.JPA_SCAN, mainClassPackage + ".**." + dataSourceName + ".dao");
        }
    }

    public static void setDefaultMybatisScanAndPackage(String dataSourceName, Map<String, String> prop) {
        serDefaultDriver(dataSourceName, prop);
        if (StringUtils.isEmpty(prop.get(Constant.MYBATIS_MAPPER_PATH))) {
            logger.info("????????? {}????????? {} ,??????????????????", dataSourceName, Constant.MYBATIS_MAPPER_PATH);
            if (Objects.equals(prop.get(Constant.DRIVER_CLASS_NAME), Constant.SQL_SERVER_DRIVER_CLASS_NAME)) {
                prop.put(Constant.MYBATIS_MAPPER_PATH, "/mapper-sqlserver/**/*.xml");
            } else {
                prop.put(Constant.MYBATIS_MAPPER_PATH, "/mapper/**/*.xml");
            }

        }
        if (StringUtils.isEmpty(prop.get(Constant.MYBATIS_SCAN))) {
            logger.info("????????? {}????????? {} ,??????????????????", dataSourceName, Constant.MYBATIS_SCAN);
            prop.put(Constant.MYBATIS_SCAN, mainClassPackage + ".**." + dataSourceName + ".mapper");
        }
    }

    /**
     * ?????????mybatis?????????
     */
    @Autowired(required = false)
    List<Interceptor> interceptorList;

    /**
     * mybatis ????????????
     *
     * @param dataSource
     * @param dataSourceName
     * @param prop
     */
    private void mybatisConfigBuild(DataSource dataSource, String dataSourceName, Map<String, String> prop) {
        //????????????SqlSessionFactoryBean??????  ???????????????mapper.xml??????
        if (Objects.equals(prop.get(Constant.MYBATIS_USE), Boolean.FALSE.toString())) {
            logger.info("????????? {} ???????????? Mybatis", dataSourceName);
            return;
        }
        // ???????????? mybatis??????
        setDefaultMybatisScanAndPackage(dataSourceName, prop);
        logger.info("Mybatis {} properties begin ", dataSourceName);
        String[] mappersPath_equipDataSource = prop.get(Constant.MYBATIS_MAPPER_PATH).split(",");
        logger.info("Mybatis Mapper path = {} ", prop.get(Constant.MYBATIS_MAPPER_PATH));

        // mybatis ????????????
        try {
            ConstructorArgumentValues cxf3 = new ConstructorArgumentValues();
            cxf3.addGenericArgumentValue(dataSource);
            setCosBean(dataSourceName + "TransactionManager", DataSourceTransactionManager.class, cxf3);
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException("??????mybatisTransactionManager??????????????????" + dataSourceName + "??????????????????");
        }
        //??????resource
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new LinkedList<>();
        for (String path : mappersPath_equipDataSource) {
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + path;
            Resource[] mapperLocations = new Resource[0];
            try {
                mapperLocations = pathMatchingResourcePatternResolver.getResources(packageSearchPath);
            } catch (IOException e) {
                logger.info("Exception = {} ", e);
                throw new RuntimeException("??????mybaits????????????????????????" + dataSourceName + "????????????mybatis-mapper-path??????????????????");
            }
            // mapper ??????????????????????????????sqlSessionFactoryBean??????????????????????????????????????????????????????
            Map<String, Resource> map = new LinkedHashMap<>();
            for (Resource resource : mapperLocations) {
                String filename = resource.getFilename();
                if (map.get(filename) == null) {
                    map.put(filename, resource);
                }
            }

            resources.addAll(map.values().stream().collect(Collectors.toList()));
        }
        if (resources.size() == 0) {
            //throw new RuntimeException(310,"?????????mybatis-mapper-path??????????????????");
            logger.error("mapper.xml?????????0????????????????????????mybatis-mapper-path??????????????????");
        }
        Map original = new HashMap<>(2);
        original.put("dataSource", dataSource);
        original.put("mapperLocations", resources.toArray((new Resource[0])));
        //??????SqlSessionFactoryBean
        Object sqlSessionFactoryBean = null;
        try {
            setBean(dataSourceName + "SqlSessionFactoryBean", SqlSessionFactoryBean.class, original);
            //??????????????????sqlSessionFactoryBean ????????????SqlSessionTemplate
            sqlSessionFactoryBean = getBean(dataSourceName + "SqlSessionFactoryBean");
            //??????mybatis??????????????????
            if (CollectionUtils.isNotEmpty(interceptorList)) {
                for (Interceptor interceptor : interceptorList) {
                    ((DefaultSqlSessionFactory) sqlSessionFactoryBean).getConfiguration().addInterceptor(interceptor);
                }
            }
            logger.info("sqlSessionFactoryBean = {} ", sqlSessionFactoryBean);
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException(dataSourceName + "SqlSessionFactoryBean" + "???????????????????????????????????????mapper.xml?????????????????????");
        }
        ConstructorArgumentValues cxf2 = new ConstructorArgumentValues();
        cxf2.addGenericArgumentValue(sqlSessionFactoryBean);
        try {
            setCosBean(dataSourceName + "SqlSessionTemplate", SqlSessionTemplate.class, dataSourceName, cxf2);
            logger.info("this dataSource mybatis is build completed");
        } catch (Exception e) {
            logger.info("Exception = {} ", e);
            throw new RuntimeException(dataSourceName + "SqlSessionTemplate" + "???????????????????????????????????????mapper.xml?????????????????????");
        }
    }

    /**
     * ???????????????Consumer
     */
    static Map<BeanDefinitionRegistry, Consumer> jpaConsumerMap = new ConcurrentHashMap<>();
    static Map<BeanDefinitionRegistry, Consumer> mybatisConsumerMap = new ConcurrentHashMap<>();

    /**
     * ???consumer?????????????????????
     *
     * @param beanDefinitionRegistry
     * @param f
     */
    public static void addMybatisConsumer(BeanDefinitionRegistry beanDefinitionRegistry, Consumer f) {
        mybatisConsumerMap.put(beanDefinitionRegistry, f);
    }

    /**
     * ???consumer?????????????????????
     *
     * @param beanDefinitionRegistry
     * @param f
     */
    public static void addJpaConsumer(BeanDefinitionRegistry beanDefinitionRegistry, Consumer f) {
        jpaConsumerMap.put(beanDefinitionRegistry, f);
    }


    public Map<String, Map<String, String>> getDatasource() {
        return datasource;
    }

    public static void setDatasource(Map<String, Map<String, String>> datasource) {
        DynamicDataSourceConfiguration.datasource = datasource;
    }

    /**
     * ??????ApplicationContextAware?????????context????????????, ????????????????????????.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        DynamicDataSourceConfiguration.applicationContext = applicationContext;
    }

    /**
     * ?????????????????????????????????ApplicationContext.
     */
    public static ApplicationContext getApplicationContext() {
        checkApplicationContext();
        return applicationContext;
    }

    /**
     * ??????spring????????????bean
     *
     * @param beanName
     */
    public static void removeBean(String beanName) {
        ApplicationContext ctx = getApplicationContext();
        DefaultListableBeanFactory acf = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();
        acf.removeBeanDefinition(beanName);
    }

    /**
     * ???????????????ApplicationContext?????????Bean, ???????????????????????????????????????.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        checkApplicationContext();
        if (applicationContext.containsBean(name)) {
            return (T) applicationContext.getBean(name);
        }
        return null;
    }

    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("applicaitonContext?????????,??????applicationContext.xml?????????SpringContextUtil");
        }
    }

    public static <T> T getBeanForMap(Class<T> clazz) {
        checkApplicationContext();
        Map<String, T> beansOfType = applicationContext.getBeansOfType(clazz);

        return beansOfType.entrySet().stream().findFirst().get().getValue();
    }

    /**
     * ??????????????????bean???ApplicationContext???
     *
     * @param beanName
     * @param clazz
     * @param original bean????????????
     */
    public static void setBean(String beanName, Class<?> clazz, Map<String, Object> original) {
        checkApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory.containsBean(beanName)) {
            return;
        }
        //BeanDefinition beanDefinition = new RootBeanDefinition(clazz);
        GenericBeanDefinition definition = new GenericBeanDefinition();
        //???class
        definition.setBeanClass(clazz);
        //????????????
        definition.setPropertyValues(new MutablePropertyValues(original));
        //?????????spring?????????
        beanFactory.registerBeanDefinition(beanName, definition);
    }

    public void setCosBean(String beanName, Class<?> clazz, String dataSourceName, ConstructorArgumentValues original) {
        checkApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        //????????????
        if (beanFactory.containsBean(beanName)) {
            return;
        }
        GenericBeanDefinition definition = new GenericBeanDefinition();
        //???class
        definition.setBeanClass(clazz);
        // ?????????????????????bean ?????????transactionManager  ???????????????primary  ?????????????????? transactionManager
        if (beanName.equals(Constant.SPRING_JPA_TRANSACTIONMANAGER_BEAN_NAME)) {
            definition.setPrimary(true);
        }
        // ???????????????????????? ?????????Primary  ??????????????? transactionManager?????????
        // ?????????jpa????????????????????????transactionManager???????????????primary ????????????????????????????????????
        if (primaryDataSource.equals(dataSourceName) && !beanName.startsWith(Constant.SPRING_JPA_TRANSACTIONMANAGER_BEAN_NAME)) {
            definition.setPrimary(true);
        }
        //????????????
        definition.setConstructorArgumentValues(new ConstructorArgumentValues(original));
        //?????????spring?????????
        beanFactory.registerBeanDefinition(beanName, definition);
    }

    public static void setCosBean(String beanName, Class<?> clazz, ConstructorArgumentValues original) {
        checkApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        //????????????
        if (beanFactory.containsBean(beanName)) {
            return;
        }
        GenericBeanDefinition definition = new GenericBeanDefinition();
        //???class
        definition.setBeanClass(clazz);
        //????????????
        definition.setConstructorArgumentValues(new ConstructorArgumentValues(original));
        //?????????spring?????????
        beanFactory.registerBeanDefinition(beanName, definition);
    }


    public void setCosAndInitBean(String beanName, Class<?> clazz, ConstructorArgumentValues original) {
        checkApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory.containsBean(beanName)) {
            return;
        }
        GenericBeanDefinition definition = new GenericBeanDefinition();
        //???class
        definition.setBeanClass(clazz);
        definition.setFactoryMethodName("getInit");
        //????????????
        definition.setConstructorArgumentValues(new ConstructorArgumentValues(original));
        //?????????spring?????????
        beanFactory.registerBeanDefinition(beanName, definition);
    }

    public void registerDataSources(String beanName, Map<String, String> config) {

        checkApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory.containsBean(beanName)) {
            return;
        }
        if (serDefaultDriver(beanName, config)) {
            return;
        }

        logger.info("register dataSoruce = {}", beanName + "DataSource");
        BeanDefinitionBuilder beanDefinitionBuilder;
        if (isUseHikari) {
            beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(HikariDataSource.class);
        } else {
            beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
            // ???????????????????????????????????????????????????
            defaultDataSourcePropMap.forEach((k, v) -> {
                if (!config.containsKey(k)) {
                    config.put(k, defaultDataSourcePropMap.get(k));
                }
            });
        }

        config.forEach((k, v) -> {
            if (k.contains("-")) {
                return;
            }
            if ("url".equals(k) && StringUtils.isNotEmpty(v) && isUseHikari) {
                if (beanDefinitionBuilder.getBeanDefinition().getPropertyValues().get("jdbcUrl") != null) {
                    return;
                }
                beanDefinitionBuilder.addPropertyValue("jdbcUrl", v);
                return;
            }
            if ("jdbcUrl".equals(k) && StringUtils.isNotEmpty(v) && !isUseHikari) {
                if (beanDefinitionBuilder.getBeanDefinition().getPropertyValues().get("url") != null) {
                    return;
                }
                beanDefinitionBuilder.addPropertyValue("url", v);
                return;
            }

            if (Constant.PASSWORD.equals(k) && StringUtils.isNotEmpty(v)) {
                if (beanDefinitionBuilder.getBeanDefinition().getPropertyValues().get(Constant.PASSWORD) != null) {
                    return;
                }
                beanDefinitionBuilder.addPropertyValue(k, v);
                return;
            }
            if (Constant.RSA_PASSWORD.equals(k) && StringUtils.isNotEmpty(v)) {
                if (beanDefinitionBuilder.getBeanDefinition().getPropertyValues().get(Constant.PASSWORD) != null) {
                    return;
                }
                try {
                    beanDefinitionBuilder.addPropertyValue(Constant.PASSWORD, RSAUtil.decode(v));
                } catch (Exception e) {
                    logger.info("Exception = {} ", e);
                    throw new RuntimeException(beanName + "???????????????rsa-password????????????");
                }
                return;
            }
            beanDefinitionBuilder.addPropertyValue(k, v);
        });
        beanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
    }

    /**
     * ??????????????????????????? ?????? url?????????
     *
     * @param beanName
     * @param config
     * @return
     */
    private static boolean serDefaultDriver(String beanName, Map<String, String> config) {
        if (StringUtils.isEmpty(config.get(Constant.DRIVER_CLASS_NAME))) {
            logger.info("????????? {}????????? {} ,??????????????????", beanName, Constant.DRIVER_CLASS_NAME);
            String url = config.get("jdbcUrl");
            if (StringUtils.isEmpty(url)) {
                url = config.get("url");
                if (StringUtils.isEmpty(url)) {
                    logger.error("????????? {} ??????url ?????????", beanName);
                    return true;
                }
            }
            if (url.contains("sqlserver")) {
                config.put(Constant.DRIVER_CLASS_NAME, Constant.SQL_SERVER_DRIVER_CLASS_NAME);
            } else {
                config.put(Constant.DRIVER_CLASS_NAME, "com.mysql.jdbc.Driver");
            }
        }
        return false;
    }

    /**
     * ?????????????????????
     */
    static {
        defaultDataSourcePropMap.put("connectionProperties", "druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000");
        defaultDataSourcePropMap.put("testWhileIdle", "true");
        defaultDataSourcePropMap.put("maxActive", "10");
        defaultDataSourcePropMap.put("minIdle", "2");
        defaultDataSourcePropMap.put("initialSize", "2");
        defaultDataSourcePropMap.put("removeAbandoned", "true");
        defaultDataSourcePropMap.put("removeAbandonedTimeout", "280");
        defaultDataSourcePropMap.put("logAbandoned", "true");
        defaultDataSourcePropMap.put("validationQuery", "select 1");
    }
}
