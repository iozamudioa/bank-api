package dev.ioza.bank.application.ports.in;

public interface Dashboard {
  DashboardView get(String householdId);
}
