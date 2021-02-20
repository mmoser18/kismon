package net.mmo.utils.kism.backend.repositories;

import net.mmo.utils.kism.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository class to access the database. Spring Boot makes this a painless process.
 * All you need to do is define an interface that describes the entity nodeType and primary key nodeType,
 * and Spring Data will configure it for you.
 */
public interface CompanyRepository extends JpaRepository<Company, Long>
{
	// empty
}