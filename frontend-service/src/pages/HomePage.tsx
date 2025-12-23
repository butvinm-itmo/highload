import React from 'react';
import { Layout } from '../components/Layout';

export function HomePage() {
  return (
    <Layout>
      <div className="text-center py-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">Welcome to Tarology</h1>
        <p className="text-lg text-gray-600">Your Tarot reading and interpretation service</p>
        <p className="text-sm text-gray-500 mt-8">Spreads feed will be implemented in Stage 4</p>
      </div>
    </Layout>
  );
}
