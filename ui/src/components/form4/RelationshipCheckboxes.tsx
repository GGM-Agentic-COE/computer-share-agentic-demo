import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';

export type RelationshipField =
  | 'relationshipDirector'
  | 'relationshipOfficer'
  | 'relationshipTenPercentOwner'
  | 'relationshipOther';

interface RelationshipCheckboxesValues {
  relationshipDirector: boolean;
  relationshipOfficer: boolean;
  relationshipTenPercentOwner: boolean;
  relationshipOther: boolean;
  officerTitle: string;
}

interface RelationshipCheckboxesProps {
  values: RelationshipCheckboxesValues;
  onToggle: (field: RelationshipField, checked: boolean) => void;
  onOfficerTitleChange: (value: string) => void;
  error?: string | null;
}

// Section 5 of the real Form 4 — real forms allow multiple relationships checked at once (e.g.
// a director who is also a 10% owner), so each checkbox toggles independently.
export function RelationshipCheckboxes({ values, onToggle, onOfficerTitleChange, error }: RelationshipCheckboxesProps) {
  return (
    <div>
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        Relationship to Issuer
      </p>
      <div className="flex flex-col gap-2.5 text-sm">
        <label className="flex items-center gap-2">
          <Checkbox
            checked={values.relationshipDirector}
            onCheckedChange={(c) => onToggle('relationshipDirector', c === true)}
          />
          Director
        </label>
        <div className="flex flex-wrap items-center gap-2">
          <label className="flex items-center gap-2">
            <Checkbox
              checked={values.relationshipOfficer}
              onCheckedChange={(c) => onToggle('relationshipOfficer', c === true)}
            />
            Officer (give title below)
          </label>
          <Input
            aria-label="Officer title"
            placeholder="Title"
            value={values.officerTitle}
            onChange={(e) => onOfficerTitleChange(e.target.value)}
            disabled={!values.relationshipOfficer}
            className="h-8 max-w-[220px] text-sm"
          />
        </div>
        <label className="flex items-center gap-2">
          <Checkbox
            checked={values.relationshipTenPercentOwner}
            onCheckedChange={(c) => onToggle('relationshipTenPercentOwner', c === true)}
          />
          10% Owner
        </label>
        <label className="flex items-center gap-2">
          <Checkbox
            checked={values.relationshipOther}
            onCheckedChange={(c) => onToggle('relationshipOther', c === true)}
          />
          Other
        </label>
      </div>
      {error && <p className="mt-2 text-xs text-destructive">{error}</p>}
    </div>
  );
}
