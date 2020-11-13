package net.mmo.utils.kism.backend.repositories;

import java.util.List;

import net.mmo.utils.kism.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 
 * Repository class to access the database. Spring Boot makes this a painless process: 
 * All you need to do is define an interface that describes the entity type and primary key type, 
 * and Spring Data will configure one for you.
 */
public interface ContactRepository extends JpaRepository<Contact, Long>
{
	// using Java Persistence Query Language (JPQL) to phrase the query:
	@Query("select c from Contact c " 
	      + "where lower(c.firstName) like lower(concat('%', :searchTerm, '%')) " 
	      + "   or lower(c.lastName)  like lower(concat('%', :searchTerm, '%'))"
	)	
	List<Contact> search(@Param("searchTerm") String searchTerm);
}
