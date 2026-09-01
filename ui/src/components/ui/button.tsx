import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex shrink-0 items-center justify-center gap-1.5 whitespace-nowrap text-sm font-semibold ' +
    'transition-colors outline-none disabled:pointer-events-none disabled:opacity-50 ' +
    'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 ' +
    '[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4',
  {
    variants: {
      variant: {
        default: 'rounded-full bg-primary text-primary-foreground hover:bg-primary/90',
        secondary: 'rounded-full bg-secondary text-secondary-foreground hover:bg-secondary/90',
        outline: 'rounded-full border border-input bg-background hover:bg-muted',
        ghost: 'rounded-md hover:bg-muted',
        destructive: 'rounded-full bg-destructive text-destructive-foreground hover:bg-destructive/90',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-10 px-5',
        sm: 'h-8 px-4 text-xs',
        lg: 'h-11 px-6',
        icon: 'h-9 w-9 rounded-full',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button ref={ref} className={cn(buttonVariants({ variant, size, className }))} {...props} />
  ),
);
Button.displayName = 'Button';

export { Button, buttonVariants };
