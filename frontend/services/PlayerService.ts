import { Player } from "../types/types";
import { apiFetch } from "./api";

const request = async (path: string, init: RequestInit = {}, fallback: string): Promise<Response> => {
  let response: Response;
  try {
    response = await apiFetch(path, init);
  } catch (error) {
    // fetch rejects with a TypeError when the backend is unreachable; token errors keep their own message.
    throw error instanceof TypeError ? new Error("Could not reach the server") : error;
  }

  if (!response.ok) {
    let message = fallback;
    try {
      const body = await response.json();
      if (typeof body?.error === "string" && body.error) message = body.error;
    } catch {
      message = fallback;
    }
    throw new Error(message);
  }
  return response;
};

const getAllPlayers = async (): Promise<Player[]> =>
  (await request("/api/players", {}, "Failed to load players")).json();

const getPlayerById = async (id: number): Promise<Player> =>
  (await request("/api/players/" + id, {}, "Player not found")).json();

const createPlayer = async (name: string, password: string): Promise<Player> =>
  (await request("/api/players", { method: "POST", body: JSON.stringify({ name, password }) }, "Failed to create player")).json();

const changePassword = async (name: string, password: string): Promise<Player> =>
  (await request("/api/players/" + encodeURIComponent(name) + "/password", {
    method: "PATCH",
    body: JSON.stringify({ password }),
  }, "Failed to change password")).json();

const addPoints = async (name: string, points: number): Promise<Player> =>
  (await request("/api/players/" + encodeURIComponent(name) + "/score", {
    method: "PATCH",
    body: JSON.stringify({ points }),
  }, "Failed to update score")).json();

const deletePlayer = async (id: number): Promise<void> => {
  await request("/api/players/" + id, { method: "DELETE" }, "Failed to delete player");
};

const login = async (name: string, password: string): Promise<boolean> => {
  let response: Response;
  try {
    response = await apiFetch("/api/players/login", {
      method: "POST",
      body: JSON.stringify({ name, password }),
    });
  } catch (error) {
    // fetch rejects with a TypeError when the backend is unreachable; token errors keep their own message.
    throw error instanceof TypeError ? new Error("Could not reach the server") : error;
  }
  if (response.status === 401) return false;
  if (!response.ok) throw new Error("Login failed, try again");
  return true;
};

export const errorText = (error: unknown, fallback: string): string =>
  error instanceof Error && error.message ? error.message : fallback;

const PlayerService = {
  getAllPlayers,
  getPlayerById,
  createPlayer,
  changePassword,
  addPoints,
  deletePlayer,
  login,
};

export default PlayerService;
