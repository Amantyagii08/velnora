import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { CartProvider } from './context/CartContext.jsx';
import { OrderProvider } from './context/OrderContext.jsx';
import { MarketplaceDataProvider } from './context/MarketplaceDataContext.jsx';
import DataSyncProvider from './context/DataSyncProvider.jsx';
import './index.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <DataSyncProvider><AuthProvider><MarketplaceDataProvider><CartProvider><OrderProvider><App /></OrderProvider></CartProvider></MarketplaceDataProvider></AuthProvider></DataSyncProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
