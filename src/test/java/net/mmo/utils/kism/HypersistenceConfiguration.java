
package net.mmo.utils.kism;

/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 18 Jul 2021
 */

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.JpaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HypersistenceConfiguration
{

	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;

	@Bean
	public HypersistenceOptimizer hypersistenceOptimizer() {
		return new HypersistenceOptimizer(new JpaConfig(this.entityManagerFactory));
	}
}
