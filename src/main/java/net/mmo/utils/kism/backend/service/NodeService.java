
package net.mmo.utils.kism.backend.service;

import java.util.List;

import net.mmo.utils.kism.backend.repositories.NodeRepository;
import net.mmo.utils.kism.entities.Node;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Companies
 */
@Service
public class NodeService
{
	private NodeRepository nodeRepository;

	public NodeService(NodeRepository nodeRepository) {
		this.nodeRepository = nodeRepository;
	}

	public List<Node> findAll() {
		return this.nodeRepository.findAll();
	}

	public List<Node> findAll(String stringFilter) {
		if (stringFilter == null || stringFilter.isEmpty()) {
			return this.nodeRepository.findAll();
		} else {
			return this.nodeRepository.search(stringFilter);
		}
	}

}
