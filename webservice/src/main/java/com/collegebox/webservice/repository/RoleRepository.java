package com.collegebox.webservice.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.collegebox.webservice.pojo.jpa.Role;

public interface RoleRepository extends PagingAndSortingRepository<Role, Long> {
	
}
