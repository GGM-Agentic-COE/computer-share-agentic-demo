import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const alertVariants = cva('flex items-start gap-2 rounded-md border px-3 py-2 text-sm', {
  variants: {
    variant: {
      default: 'border-border bg-background text-foreground',
      destructive: 'border-status-incomplete/40 bg-status-incomplete/10 text-status-incomplete',
    },
  },
  defaultVariants: { variant: 'default' },
});

export interface AlertProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof alertVariants> {}

const Alert = React.forwardRef<HTMLDivElement, AlertProps>(({ className, variant, ...props }, ref) => (
  <div ref={ref} role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
));
Alert.displayName = 'Alert';

const AlertDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, ...props }, ref) => <div ref={ref} className={cn('leading-snug', className)} {...props} />,
);
AlertDescription.displayName = 'AlertDescription';

export { Alert, AlertDescription };
