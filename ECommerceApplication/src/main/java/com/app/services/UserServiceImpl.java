package com.app.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.config.AppConstants;
import com.app.entites.Address;
import com.app.entites.Cart;
import com.app.entites.Role;
import com.app.entites.User;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.AddressDTO;
import com.app.payloads.UserDTO;
import com.app.repositories.AddressRepo;
import com.app.repositories.RoleRepo;
import com.app.repositories.UserRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private AddressRepo addressRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public UserDTO registerUser(UserDTO userDTO) {

		try {
			User user = modelMapper.map(userDTO, User.class);

			Cart cart = new Cart();
			user.setCart(cart);

			Role role = roleRepo.findById(AppConstants.USER_ID).get();
			user.getRoles().add(role);

			String country = userDTO.getAddress().getCountry();
			String state = userDTO.getAddress().getState();
			String city = userDTO.getAddress().getCity();
			String pincode = userDTO.getAddress().getPincode();
			String street = userDTO.getAddress().getStreet();
			String buidlingName = userDTO.getAddress().getBuildingName();

			Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state,
					city, street, pincode, buidlingName);

			if (address == null) {
				address = new Address(country, state, city, street, pincode, buidlingName);

				address = addressRepo.save(address);
			}

			user.setAddresses(List.of(address));

			User registeredUser = userRepo.save(user);

			cart.setUser(registeredUser);
			
			userDTO = modelMapper.map(registeredUser, UserDTO.class);

			userDTO.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));

			return userDTO;
		} catch (DataIntegrityViolationException e) {
			throw new APIException("User already exists with emailId: " + userDTO.getEmail());
		}

	}

	@Override
	public List<UserDTO> getAllUsers() {
		List<User> users = userRepo.findAll();

		if(users.size() == 0) {
			throw new APIException("No User exists !!!");
		}
		
		List<UserDTO> userDTOs = users.stream().map(user -> {
			UserDTO dto = modelMapper.map(user, UserDTO.class);

			dto.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));

			return dto;

		}).collect(Collectors.toList());

		return userDTOs;
	}
	
	@Override
	public UserDTO getUserById(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		UserDTO userDTO = modelMapper.map(user, UserDTO.class);

		userDTO.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));

		return userDTO;
	}

	@Override
	public UserDTO updateUser(Long userId, UserDTO userDTO) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		user.setFirstName(userDTO.getFirstName());
		user.setLastName(userDTO.getLastName());
		user.setMobileNumber(userDTO.getMobileNumber());
		user.setEmail(userDTO.getEmail());
		user.setPassword(userDTO.getPassword());

		String country = userDTO.getAddress().getCountry();
		String state = userDTO.getAddress().getState();
		String city = userDTO.getAddress().getCity();
		String pincode = userDTO.getAddress().getPincode();
		String street = userDTO.getAddress().getStreet();
		String buidlingName = userDTO.getAddress().getBuildingName();

		Address address = addressRepo.findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(country, state,
				city, street, pincode, buidlingName);

		if (address == null) {
			address = new Address(country, state, city, street, pincode, buidlingName);

			address = addressRepo.save(address);

			user.setAddresses(List.of(address));
		}

		userDTO = modelMapper.map(user, UserDTO.class);

		userDTO.setAddress(modelMapper.map(user.getAddresses().stream().findFirst().get(), AddressDTO.class));

		return userDTO;
	}

	@Override
	public String deleteUser(Long userId) {
		User user = userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

		userRepo.delete(user);

		return "User with userId " + userId + " deleted successfully!!!";
	}

}
