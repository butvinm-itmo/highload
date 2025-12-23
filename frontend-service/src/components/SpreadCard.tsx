import React from 'react';
import { Link } from 'react-router-dom';
import { SpreadSummaryDto } from '../types';

interface SpreadCardProps {
  spread: SpreadSummaryDto;
}

export function SpreadCard({ spread }: SpreadCardProps) {
  const formattedDate = new Date(spread.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <Link to={`/spreads/${spread.id}`}>
      <div className="bg-white rounded-lg shadow hover:shadow-md transition-shadow p-6 border border-gray-200">
        <div className="flex justify-between items-start mb-3">
          <h3 className="text-lg font-semibold text-gray-900 flex-1">{spread.question}</h3>
          <span className="ml-4 px-2 py-1 text-xs font-medium text-indigo-600 bg-indigo-100 rounded">
            {spread.layoutTypeName}
          </span>
        </div>

        <div className="flex items-center text-sm text-gray-500 space-x-4">
          <div className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <span>{spread.authorUsername}</span>
          </div>

          <div className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
            </svg>
            <span>{spread.cardsCount} cards</span>
          </div>

          <div className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
            </svg>
            <span>{spread.interpretationsCount} interpretations</span>
          </div>
        </div>

        <div className="mt-3 text-xs text-gray-400">{formattedDate}</div>
      </div>
    </Link>
  );
}
