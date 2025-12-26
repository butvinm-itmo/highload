import { motion } from 'framer-motion';

interface LoadingProps {
  message?: string;
  fullScreen?: boolean;
}

export function Loading({ message = 'Loading...', fullScreen = false }: LoadingProps) {
  const containerClass = fullScreen
    ? 'min-h-screen flex items-center justify-center'
    : 'flex justify-center py-8';

  return (
    <div className={containerClass}>
      <div className="text-center">
        <motion.div className="relative inline-block mb-6">
          {/* Outer ring */}
          <motion.div
            className="absolute inset-0 rounded-full h-16 w-16 border-2 border-transparent border-t-mystic-500 border-r-cosmic-500"
            animate={{ rotate: 360 }}
            transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
          />
          {/* Inner ring */}
          <motion.div
            className="rounded-full h-16 w-16 border-2 border-transparent border-b-cosmic-600 border-l-mystic-600"
            animate={{ rotate: -360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          />
          {/* Center glow */}
          <motion.div
            className="absolute inset-0 rounded-full blur-md"
            style={{ background: 'linear-gradient(to right, rgba(124, 58, 237, 0.2), rgba(192, 38, 211, 0.2))' }}
            animate={{
              scale: [1, 1.2, 1],
              opacity: [0.5, 0.8, 0.5]
            }}
            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
          />
        </motion.div>
        <motion.p
          className="text-gray-300 font-serif text-sm tracking-wide"
          initial={{ opacity: 0 }}
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
        >
          {message}
        </motion.p>
      </div>
    </div>
  );
}
