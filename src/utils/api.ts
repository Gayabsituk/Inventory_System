import { projectId, publicAnonKey } from './supabase/info';

const API_BASE_URL = `https://${projectId}.supabase.co/functions/v1/make-server-9f945771`;

// Store access token
let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
  if (token) {
    localStorage.setItem('k4j_access_token', token);
  } else {
    localStorage.removeItem('k4j_access_token');
  }
};

export const getAccessToken = () => {
  if (!accessToken) {
    accessToken = localStorage.getItem('k4j_access_token');
  }
  return accessToken;
};

const getHeaders = (includeAuth = true) => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (includeAuth) {
    const token = getAccessToken();
    headers['Authorization'] = `Bearer ${token || publicAnonKey}`;
  } else {
    headers['Authorization'] = `Bearer ${publicAnonKey}`;
  }

  return headers;
};

// ============================================
// AUTH API
// ============================================

export const signUp = async (username: string, password: string, role: string) => {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/signup`, {
      method: 'POST',
      headers: getHeaders(false),
      body: JSON.stringify({ username, password, role }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Signup failed');
    }

    return data;
  } catch (error) {
    console.error('Signup error:', error);
    throw error;
  }
};

export const signIn = async (username: string, password: string) => {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/signin`, {
      method: 'POST',
      headers: getHeaders(false),
      body: JSON.stringify({ username, password }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Sign in failed');
    }

    if (data.accessToken) {
      setAccessToken(data.accessToken);
    }

    return data;
  } catch (error) {
    console.error('Sign in error:', error);
    throw error;
  }
};

export const checkSession = async () => {
  try {
    const token = getAccessToken();
    if (!token) {
      return null;
    }

    const response = await fetch(`${API_BASE_URL}/auth/session`, {
      method: 'GET',
      headers: getHeaders(true),
    });

    const data = await response.json();

    if (!response.ok) {
      setAccessToken(null);
      return null;
    }

    return data.user;
  } catch (error) {
    console.error('Session check error:', error);
    setAccessToken(null);
    return null;
  }
};

export const signOut = async () => {
  try {
    await fetch(`${API_BASE_URL}/auth/signout`, {
      method: 'POST',
      headers: getHeaders(true),
    });

    setAccessToken(null);
  } catch (error) {
    console.error('Sign out error:', error);
    setAccessToken(null);
  }
};

// ============================================
// PRODUCTS API
// ============================================

export const getProducts = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/products`, {
      method: 'GET',
      headers: getHeaders(false),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to get products');
    }

    return data.products;
  } catch (error) {
    console.error('Get products error:', error);
    throw error;
  }
};

export const addProduct = async (product: any) => {
  try {
    const response = await fetch(`${API_BASE_URL}/products`, {
      method: 'POST',
      headers: getHeaders(true),
      body: JSON.stringify(product),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to add product');
    }

    return data.product;
  } catch (error) {
    console.error('Add product error:', error);
    throw error;
  }
};

export const updateProduct = async (productId: string, updates: any) => {
  try {
    const response = await fetch(`${API_BASE_URL}/products/${productId}`, {
      method: 'PUT',
      headers: getHeaders(true),
      body: JSON.stringify(updates),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to update product');
    }

    return data.product;
  } catch (error) {
    console.error('Update product error:', error);
    throw error;
  }
};

export const deleteProduct = async (productId: string) => {
  try {
    const response = await fetch(`${API_BASE_URL}/products/${productId}`, {
      method: 'DELETE',
      headers: getHeaders(true),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to delete product');
    }

    return data;
  } catch (error) {
    console.error('Delete product error:', error);
    throw error;
  }
};

// ============================================
// USERS API
// ============================================

export const getUsers = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/users`, {
      method: 'GET',
      headers: getHeaders(true),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to get users');
    }

    return data.users;
  } catch (error) {
    console.error('Get users error:', error);
    throw error;
  }
};

export const updateUser = async (userId: string, updates: any) => {
  try {
    const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
      method: 'PUT',
      headers: getHeaders(true),
      body: JSON.stringify(updates),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to update user');
    }

    return data.user;
  } catch (error) {
    console.error('Update user error:', error);
    throw error;
  }
};

export const deleteUser = async (userId: string) => {
  try {
    const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
      method: 'DELETE',
      headers: getHeaders(true),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to delete user');
    }

    return data;
  } catch (error) {
    console.error('Delete user error:', error);
    throw error;
  }
};

// ============================================
// INITIALIZATION API
// ============================================

export const initializeDatabase = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/init`, {
      method: 'POST',
      headers: getHeaders(false),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Initialization failed');
    }

    return data;
  } catch (error) {
    console.error('Initialize error:', error);
    throw error;
  }
};
