import { Player } from "../types/types";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "";

const getAllPlayers = async (): Promise<Player[]> => {
  const response = await fetch(BASE + "/api/players");
  if (!response.ok) throw new Error("Failed to fetch players");
  return response.json();
};

const getPlayerById = async (id: number): Promise<Player> => {
  const response = await fetch(BASE + "/api/players/" + id);
  if (!response.ok) throw new Error("Player not found");
  return response.json();
};

const createPlayer = async (name: string, password: string): Promise<Player> => {
  const response = await fetch(BASE + "/api/players", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, password }),
  });
  if (!response.ok) throw new Error("Failed to create player");
  return response.json();
};

const changePassword = async (name: string, password: string): Promise<Player> => {
  const response = await fetch(BASE + "/api/players/" + name + "/password", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password }),
  });
  if (!response.ok) throw new Error("Failed to change password");
  return response.json();
};

const addPoints = async (name: string, points: number): Promise<Player> => {
  const response = await fetch(BASE + "/api/players/" + name + "/score", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ points }),
  });
  if (!response.ok) throw new Error("Failed to update score");
  return response.json();
};

const deletePlayer = async (id: number): Promise<void> => {
  const response = await fetch(BASE + "/api/players/" + id, { method: "DELETE" });
  if (!response.ok) throw new Error("Failed to delete player");
};

export default {
  getAllPlayers,
  getPlayerById,
  createPlayer,
  changePassword,
  addPoints,
  deletePlayer,
};