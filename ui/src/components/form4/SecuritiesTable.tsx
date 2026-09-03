import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

export interface SecuritiesTableValues {
  titleOfSecurity: string;
  transactionDate: string;
  transactionCode: string;
  shares: string;
  acquiredOrDisposed: 'A' | 'D';
  pricePerShare: string;
  sharesOwnedFollowingTransaction: string;
  ownershipForm: 'D' | 'I';
}

interface SecuritiesTableProps {
  values: SecuritiesTableValues;
  onChange: <K extends keyof SecuritiesTableValues>(field: K, value: SecuritiesTableValues[K]) => void;
  errors: Partial<Record<keyof SecuritiesTableValues, string>>;
}

// Table I of the real Form 4 — non-derivative securities acquired/disposed/beneficially owned.
export function SecuritiesTable({ values, onChange, errors }: SecuritiesTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Title of Security</TableHead>
          <TableHead>Transaction Date</TableHead>
          <TableHead>Code</TableHead>
          <TableHead>Shares</TableHead>
          <TableHead>Acquired/Disposed</TableHead>
          <TableHead>Price</TableHead>
          <TableHead>Owned Following</TableHead>
          <TableHead>Ownership Form</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow className="hover:bg-transparent">
          <TableCell>
            <Input
              aria-label="Title of security"
              value={values.titleOfSecurity}
              onChange={(e) => onChange('titleOfSecurity', e.target.value)}
              invalid={Boolean(errors.titleOfSecurity)}
              className="h-9 min-w-[130px] text-sm"
            />
            {errors.titleOfSecurity && <p className="mt-1 text-xs text-destructive">{errors.titleOfSecurity}</p>}
          </TableCell>
          <TableCell>
            <Input
              aria-label="Transaction date"
              type="date"
              value={values.transactionDate}
              onChange={(e) => onChange('transactionDate', e.target.value)}
              invalid={Boolean(errors.transactionDate)}
              className="h-9 min-w-[150px] text-sm"
            />
          </TableCell>
          <TableCell>
            <Input
              aria-label="Transaction code"
              maxLength={2}
              value={values.transactionCode}
              onChange={(e) => onChange('transactionCode', e.target.value.toUpperCase())}
              invalid={Boolean(errors.transactionCode)}
              className="h-9 w-16 text-sm"
            />
          </TableCell>
          <TableCell>
            <Input
              id="shares"
              aria-label="Shares"
              value={values.shares}
              onChange={(e) => onChange('shares', e.target.value)}
              invalid={Boolean(errors.shares)}
              className="h-9 w-24 text-sm"
            />
            {errors.shares && <p className="mt-1 text-xs text-destructive">{errors.shares}</p>}
          </TableCell>
          <TableCell>
            <Select value={values.acquiredOrDisposed} onValueChange={(v) => onChange('acquiredOrDisposed', v as 'A' | 'D')}>
              <SelectTrigger
                aria-label="Acquired (A) or Disposed of (D)"
                invalid={Boolean(errors.acquiredOrDisposed)}
                className="h-9 w-20 text-sm"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="A">A</SelectItem>
                <SelectItem value="D">D</SelectItem>
              </SelectContent>
            </Select>
          </TableCell>
          <TableCell>
            <Input
              id="pricePerShare"
              aria-label="Price per share"
              value={values.pricePerShare}
              onChange={(e) => onChange('pricePerShare', e.target.value)}
              invalid={Boolean(errors.pricePerShare)}
              className="h-9 w-24 text-sm"
            />
            {errors.pricePerShare && <p className="mt-1 text-xs text-destructive">{errors.pricePerShare}</p>}
          </TableCell>
          <TableCell>
            <Input
              aria-label="Shares owned following transaction"
              value={values.sharesOwnedFollowingTransaction}
              onChange={(e) => onChange('sharesOwnedFollowingTransaction', e.target.value)}
              invalid={Boolean(errors.sharesOwnedFollowingTransaction)}
              className="h-9 w-28 text-sm"
            />
          </TableCell>
          <TableCell>
            <Select value={values.ownershipForm} onValueChange={(v) => onChange('ownershipForm', v as 'D' | 'I')}>
              <SelectTrigger
                aria-label="Ownership form: Direct (D) or Indirect (I)"
                invalid={Boolean(errors.ownershipForm)}
                className="h-9 w-20 text-sm"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="D">D</SelectItem>
                <SelectItem value="I">I</SelectItem>
              </SelectContent>
            </Select>
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  );
}
