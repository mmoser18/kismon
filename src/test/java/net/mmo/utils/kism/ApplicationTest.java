/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 18 Jul 2021
 */

package net.mmo.utils.kism;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApplicationTest
{
	@Autowired
	private HypersistenceOptimizer hypersistenceOptimizer;

//	@Autowired
//	private LinkService linkService;
//
//	@Test
//	void shouldReturnPagedLinks() {
//		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
//		LinksDTO linksDTO = linkService.getAllLinks(pageable);
//		assertThat(linksDTO.getData()).isNotEmpty();
//	}

	@AfterEach
	void afterEach() {
		assertTrue(this.hypersistenceOptimizer.getEvents().isEmpty());
	}
}