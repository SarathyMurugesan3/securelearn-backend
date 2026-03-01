package com.example.demo.user.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
	@Id
	private String id;
	private String name;
	@Indexed(unique = true)
	private String email;
	private String password;
	private String role;
	@Indexed
	private int riskScore;
	@Indexed
	private boolean blocked;
	private List<String> deviceFingerprints = new ArrayList<>();
	
	private List<DeviceInfo> devices = new ArrayList<>();
	
	public User() {}
	public User( String name, String email, String password, String role) {
		super();
		this.name = name;
		this.email = email;
		this.password = password;
		this.role = role;
	}
	public User(String id, String name, String email, String password, String role, int riskScore, boolean blocked,
			List<String> deviceFingerprints) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.password = password;
		this.role = role;
		this.riskScore = riskScore;
		this.blocked = blocked;
		this.deviceFingerprints = deviceFingerprints;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public int getRiskScore() {
		return riskScore;
	}
	public void setRiskScore(int riskScore) {
		this.riskScore = riskScore;
	}
	public boolean isBlocked() {
		return blocked;
	}
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
	public List<String> getDeviceFingerprints() {
		return deviceFingerprints;
	}
	public void setDeviceFingerprints(List<String> deviceFingerprints) {
		this.deviceFingerprints = deviceFingerprints;
	}
	public List<DeviceInfo> getDevices() {
		return devices;
	}
	public void setDevices(List<DeviceInfo> devices) {
		this.devices = devices;
	}
	
	
	
	
}
