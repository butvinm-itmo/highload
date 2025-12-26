import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import type { SpreadSummaryDto } from '../types';
import { fadeInUp } from '../utils/animations';

interface SpreadCardProps {
  spread: SpreadSummaryDto;
  delay?: number;
}

export function SpreadCard({ spread, delay = 0 }: SpreadCardProps) {
  const formattedDate = new Date(spread.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <Link to={`/spreads/${spread.id}`}>
      <motion.div
        className="mystical-card p-6 group cursor-pointer"
        variants={fadeInUp}
        initial="hidden"
        animate="visible"
        transition={{ delay }}
        whileHover={{ y: -6, transition: { duration: 0.3 } }}
        whileTap={{ scale: 0.98 }}
      >
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xl font-display font-semibold text-gray-100 flex-1 group-hover:text-mystic-300 transition-colors">
            {spread.question}
          </h3>
          <span className="ml-4 px-3 py-1.5 text-xs font-accent font-semibold text-gold-300 rounded-full shadow-gold" style={{ background: 'linear-gradient(to right, rgba(76, 29, 149, 0.8), rgba(112, 26, 117, 0.8))', borderWidth: '1px', borderColor: 'rgba(245, 158, 11, 0.3)' }}>
            {spread.layoutTypeName}
          </span>
        </div>

        <div className="flex items-center text-sm text-gray-400 space-x-6 font-serif">
          <div className="flex items-center group/item">
            <svg className="w-4 h-4 mr-2 text-mystic-400 group-hover/item:text-mystic-300 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <span className="group-hover/item:text-gray-300 transition-colors">{spread.authorUsername}</span>
          </div>

          <div className="flex items-center group/item">
            <svg className="w-4 h-4 mr-2 text-cosmic-400 group-hover/item:text-cosmic-300 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
            </svg>
            <span className="group-hover/item:text-gray-300 transition-colors">{spread.cardsCount} cards</span>
          </div>

          <div className="flex items-center group/item">
            <svg className="w-4 h-4 mr-2 text-gold-400 group-hover/item:text-gold-300 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
            </svg>
            <span className="group-hover/item:text-gray-300 transition-colors">{spread.interpretationsCount} interpretations</span>
          </div>
        </div>

        <div className="mt-4 text-xs font-accent text-mystic-500 italic">{formattedDate}</div>
      </motion.div>
    </Link>
  );
}
