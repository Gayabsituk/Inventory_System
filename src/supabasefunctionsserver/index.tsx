import { Hono } from "npm:hono";
import { cors } from "npm:hono/cors";
import { logger } from "npm:hono/logger";
import { createClient } from "npm:@supabase/supabase-js@2";
import * as kv from './kv_store.tsx';

const app = new Hono();

// Middleware
app.use('*', cors());
app.use('*', logger(console.log));

// Initialize Supabase client
const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
);

// ============================================
// AUTHENTICATION ROUTES
// ============================================

// Sign up new user
app.post('/make-server-9f945771/auth/signup', async (c) => {
  try {
    const { username, password, role } = await c.req.json();

    if (!username || !password || !role) {
      return c.json({ error: 'Username, password, and role are required' }, 400);
    }

    // Check if username already exists in KV store
    const existingUsers = await kv.getByPrefix('user:');
    const userExists = existingUsers.some((u: any) => u.username === username);

    if (userExists) {
      return c.json({ error: 'Username already exists' }, 400);
    }

    // Create user in Supabase Auth
    const email = `${username}@k4jlpg.local`; // Generate email from username
    const { data: authData, error: authError } = await supabase.auth.admin.createUser({
      email,
      password,
      email_confirm: true, // Auto-confirm since we don't have email server
    });

    if (authError) {
      console.log('Auth creation error:', authError);
      return c.json({ error: `Failed to create user: ${authError.message}` }, 400);
    }

    // Store user data in KV store
    const userId = authData.user.id;
    const userData = {
      id: userId,
      username,
      role,
      createdAt: new Date().toISOString(),
    };

    await kv.set(`user:${userId}`, userData);

    return c.json({ 
      success: true, 
      user: userData,
      message: 'User created successfully' 
    });

  } catch (error) {
    console.log('Signup error:', error);
    return c.json({ error: `Signup failed: ${error.message}` }, 500);
  }
});

// Sign in user
app.post('/make-server-9f945771/auth/signin', async (c) => {
  try {
    const { username, password } = await c.req.json();

    if (!username || !password) {
      return c.json({ error: 'Username and password are required' }, 400);
    }

    const email = `${username}@k4jlpg.local`;

    // Sign in with Supabase Auth
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (authError) {
      console.log('Sign in error:', authError);
      return c.json({ error: 'Invalid username or password' }, 401);
    }

    // Get user data from KV store
    const userId = authData.user.id;
    const userData = await kv.get(`user:${userId}`);

    if (!userData) {
      return c.json({ error: 'User data not found' }, 404);
    }

    return c.json({
      success: true,
      accessToken: authData.session.access_token,
      user: userData,
    });

  } catch (error) {
    console.log('Sign in error:', error);
    return c.json({ error: `Sign in failed: ${error.message}` }, 500);
  }
});

// Get current session
app.get('/make-server-9f945771/auth/session', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];

    if (!accessToken) {
      return c.json({ error: 'No access token provided' }, 401);
    }

    const { data: { user }, error } = await supabase.auth.getUser(accessToken);

    if (error || !user) {
      return c.json({ error: 'Invalid or expired session' }, 401);
    }

    // Get user data from KV store
    const userData = await kv.get(`user:${user.id}`);

    if (!userData) {
      return c.json({ error: 'User data not found' }, 404);
    }

    return c.json({ success: true, user: userData });

  } catch (error) {
    console.log('Session check error:', error);
    return c.json({ error: `Session check failed: ${error.message}` }, 500);
  }
});

// Sign out
app.post('/make-server-9f945771/auth/signout', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];

    if (accessToken) {
      await supabase.auth.admin.signOut(accessToken);
    }

    return c.json({ success: true, message: 'Signed out successfully' });

  } catch (error) {
    console.log('Sign out error:', error);
    return c.json({ error: `Sign out failed: ${error.message}` }, 500);
  }
});

// ============================================
// PRODUCT ROUTES
// ============================================

// Get all products
app.get('/make-server-9f945771/products', async (c) => {
  try {
    const products = await kv.getByPrefix('product:');
    return c.json({ success: true, products: products || [] });
  } catch (error) {
    console.log('Get products error:', error);
    return c.json({ error: `Failed to get products: ${error.message}` }, 500);
  }
});

// Add new product
app.post('/make-server-9f945771/products', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    // Check if user is admin
    const userData = await kv.get(`user:${user.id}`);
    if (!userData || userData.role !== 'admin') {
      return c.json({ error: 'Admin access required' }, 403);
    }

    const productData = await c.req.json();
    const productId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    
    const product = {
      id: productId,
      ...productData,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    await kv.set(`product:${productId}`, product);

    return c.json({ success: true, product });

  } catch (error) {
    console.log('Add product error:', error);
    return c.json({ error: `Failed to add product: ${error.message}` }, 500);
  }
});

// Update product
app.put('/make-server-9f945771/products/:id', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    const productId = c.req.param('id');
    const updates = await c.req.json();

    // Get existing product
    const existingProduct = await kv.get(`product:${productId}`);
    if (!existingProduct) {
      return c.json({ error: 'Product not found' }, 404);
    }

    const updatedProduct = {
      ...existingProduct,
      ...updates,
      id: productId, // Ensure ID doesn't change
      updatedAt: new Date().toISOString(),
    };

    await kv.set(`product:${productId}`, updatedProduct);

    return c.json({ success: true, product: updatedProduct });

  } catch (error) {
    console.log('Update product error:', error);
    return c.json({ error: `Failed to update product: ${error.message}` }, 500);
  }
});

// Delete product
app.delete('/make-server-9f945771/products/:id', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    // Check if user is admin
    const userData = await kv.get(`user:${user.id}`);
    if (!userData || userData.role !== 'admin') {
      return c.json({ error: 'Admin access required' }, 403);
    }

    const productId = c.req.param('id');
    await kv.del(`product:${productId}`);

    return c.json({ success: true, message: 'Product deleted successfully' });

  } catch (error) {
    console.log('Delete product error:', error);
    return c.json({ error: `Failed to delete product: ${error.message}` }, 500);
  }
});

// ============================================
// USER MANAGEMENT ROUTES (Admin only)
// ============================================

// Get all users
app.get('/make-server-9f945771/users', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    // Check if user is admin
    const userData = await kv.get(`user:${user.id}`);
    if (!userData || userData.role !== 'admin') {
      return c.json({ error: 'Admin access required' }, 403);
    }

    const users = await kv.getByPrefix('user:');
    return c.json({ success: true, users: users || [] });

  } catch (error) {
    console.log('Get users error:', error);
    return c.json({ error: `Failed to get users: ${error.message}` }, 500);
  }
});

// Update user
app.put('/make-server-9f945771/users/:id', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    // Check if user is admin
    const userData = await kv.get(`user:${user.id}`);
    if (!userData || userData.role !== 'admin') {
      return c.json({ error: 'Admin access required' }, 403);
    }

    const userId = c.req.param('id');
    const updates = await c.req.json();

    // Get existing user
    const existingUser = await kv.get(`user:${userId}`);
    if (!existingUser) {
      return c.json({ error: 'User not found' }, 404);
    }

    const updatedUser = {
      ...existingUser,
      ...updates,
      id: userId, // Ensure ID doesn't change
      updatedAt: new Date().toISOString(),
    };

    await kv.set(`user:${userId}`, updatedUser);

    return c.json({ success: true, user: updatedUser });

  } catch (error) {
    console.log('Update user error:', error);
    return c.json({ error: `Failed to update user: ${error.message}` }, 500);
  }
});

// Delete user
app.delete('/make-server-9f945771/users/:id', async (c) => {
  try {
    const accessToken = c.req.header('Authorization')?.split(' ')[1];
    const { data: { user }, error: authError } = await supabase.auth.getUser(accessToken);

    if (authError || !user) {
      return c.json({ error: 'Unauthorized' }, 401);
    }

    // Check if user is admin
    const userData = await kv.get(`user:${user.id}`);
    if (!userData || userData.role !== 'admin') {
      return c.json({ error: 'Admin access required' }, 403);
    }

    const userId = c.req.param('id');

    // Prevent admin from deleting themselves
    if (userId === user.id) {
      return c.json({ error: 'Cannot delete your own account' }, 400);
    }

    // Delete from Auth
    await supabase.auth.admin.deleteUser(userId);

    // Delete from KV store
    await kv.del(`user:${userId}`);

    return c.json({ success: true, message: 'User deleted successfully' });

  } catch (error) {
    console.log('Delete user error:', error);
    return c.json({ error: `Failed to delete user: ${error.message}` }, 500);
  }
});

// ============================================
// INITIALIZATION ROUTE
// ============================================

// Initialize database with default data
app.post('/make-server-9f945771/init', async (c) => {
  try {
    // Check if already initialized
    const existingProducts = await kv.getByPrefix('product:');
    if (existingProducts && existingProducts.length > 0) {
      return c.json({ success: true, message: 'Database already initialized' });
    }

    // Create default admin user
    const adminEmail = 'admin@k4jlpg.local';
    const adminPassword = 'admin123';
    
    const { data: adminAuth, error: adminError } = await supabase.auth.admin.createUser({
      email: adminEmail,
      password: adminPassword,
      email_confirm: true,
    });

    if (adminError && !adminError.message.includes('already registered')) {
      console.log('Admin creation error:', adminError);
      return c.json({ error: `Failed to create admin: ${adminError.message}` }, 400);
    }

    if (adminAuth?.user) {
      await kv.set(`user:${adminAuth.user.id}`, {
        id: adminAuth.user.id,
        username: 'admin',
        role: 'admin',
        createdAt: new Date().toISOString(),
      });
    }

    // Create default staff user
    const staffEmail = 'staff@k4jlpg.local';
    const staffPassword = 'staff123';
    
    const { data: staffAuth, error: staffError } = await supabase.auth.admin.createUser({
      email: staffEmail,
      password: staffPassword,
      email_confirm: true,
    });

    if (staffError && !staffError.message.includes('already registered')) {
      console.log('Staff creation error:', staffError);
    }

    if (staffAuth?.user) {
      await kv.set(`user:${staffAuth.user.id}`, {
        id: staffAuth.user.id,
        username: 'staff',
        role: 'staff',
        createdAt: new Date().toISOString(),
      });
    }

    // Initialize default products
    const defaultProducts = [
      { name: "11kg Brent Gas", category: "Gas Tank", quantity: 15, price: 950.00 },
      { name: "22kg Superkalan Gas", category: "Gas Tank", quantity: 25, price: 1850.00 },
      { name: "2.7kg Superkalan", category: "Gas Tank", quantity: 18, price: 450.00 },
      { name: "LPG Hose", category: "Accessories", quantity: 50, price: 150.00 },
      { name: "LPG Regulator", category: "Accessories", quantity: 35, price: 280.00 },
      { name: "Gas Stove Burner", category: "Accessories", quantity: 20, price: 320.00 },
      { name: "O-ring", category: "Accessories", quantity: 100, price: 25.00 },
      { name: "Gas Clamp", category: "Accessories", quantity: 75, price: 35.00 },
      { name: "Double Burner Stove", category: "Stove", quantity: 12, price: 1850.00 },
      { name: "Megakalan", category: "Stove", quantity: 8, price: 2500.00 },
    ];

    for (const product of defaultProducts) {
      const productId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      await kv.set(`product:${productId}`, {
        id: productId,
        ...product,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }

    return c.json({ 
      success: true, 
      message: 'Database initialized successfully with default users and products' 
    });

  } catch (error) {
    console.log('Initialization error:', error);
    return c.json({ error: `Initialization failed: ${error.message}` }, 500);
  }
});

// Health check
app.get('/make-server-9f945771/health', (c) => {
  return c.json({ status: 'ok', message: 'K4J LPG Center API is running' });
});

Deno.serve(app.fetch);
