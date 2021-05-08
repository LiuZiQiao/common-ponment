package com.lxk.project.common.db;

import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @Author macos·lxk
 * @create 2021/5/6 下午9:34
 */

public class EntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean{
    /**
     * 核心方法实现EntityManagerFactoryBean的创建
     * @param dataSource
     * @param builder
     * @param jpaProperties
     * @param dataSourceName
     * @param modelPackage
     * @return
     */
    public static LocalContainerEntityManagerFactoryBean getInit(DataSource dataSource, EntityManagerFactoryBuilder builder, JpaProperties jpaProperties, String dataSourceName, String ... modelPackage){
        return builder
                .dataSource(dataSource)
                .properties(getVendorProperties(dataSource,jpaProperties))
                .packages(modelPackage)
                .persistenceUnit("primaryPersistenceUnit4"+dataSourceName)
                .build();
    }
    private static Map<String, Object> getVendorProperties(DataSource dataSource, JpaProperties jpaProperties) {
        return jpaProperties.getHibernateProperties(new HibernateSettings());
    }
}
