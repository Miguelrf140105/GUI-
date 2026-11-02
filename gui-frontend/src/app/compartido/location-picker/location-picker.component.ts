import { Component, Input, forwardRef, OnChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, NG_VALIDATORS, Validator } from '@angular/forms';
import { Row } from '../../models/shared.model';
import { locationPath } from '../ubicaciones.util';

interface Step {
  label: string;
  options: Row[];
  value: number | null;
}
@Component({
  selector: 'gui-location-picker',
  templateUrl: './location-picker.component.html',
  styles: [
    ':host{display:block;width:100%}.location-levels{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px}.location-path{display:block;margin-top:8px;color:var(--text-muted,#58716b)}',
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LocationPickerComponent),
      multi: true,
    },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => LocationPickerComponent), multi: true },
  ],
})
export class LocationPickerComponent implements ControlValueAccessor, Validator, OnChanges {
  @Input() locations: Row[] = [];
  @Input() label = 'Ubicación';
  @Input() required = false;
  @Input() autoSelectBranch = false;
  steps: Step[] = [];
  path: number[] = [];
  value: number | null = null;
  disabled = false;
  change: (value: number | null) => void = () => {};
  touch: () => void = () => {};
  validatorChange = () => {};
  ngOnChanges(): void {
    this.writeValue(this.value);
    this.validatorChange();
  }
  children(id: number): Row[] {
    return this.locations.filter((row) => row.parentId === id);
  }
  writeValue(value: number | null): void {
    this.value = value ?? null;
    const chain: number[] = [],
      seen = new Set<number>();
    let row = this.locations.find((item) => item.id === this.value);
    while (row && !seen.has(row.id)) {
      seen.add(row.id);
      chain.unshift(row.id);
      row = this.locations.find((item) => item.id === row!.parentId);
    }
    if (chain.length >= 2 && !this.children(chain[1]).length) chain.splice(1, 0, 0);
    this.path = chain;
    this.rebuild();
  }
  registerOnChange(fn: (value: number | null) => void): void {
    this.change = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.touch = fn;
  }
  registerOnValidatorChange(fn: () => void): void {
    this.validatorChange = fn;
  }
  setDisabledState(value: boolean): void {
    this.disabled = value;
  }
  validate(): { required?: boolean; location?: boolean } | null {
    if (this.value === null) return this.required ? { required: true } : null;
    return this.locations.some((row) => row.id === this.value) ? null : { location: true };
  }
  choose(index: number, id: number | null): void {
    if (this.disabled) return;
    this.path = this.path.slice(0, index);
    if (id !== null) this.path.push(id);
    this.value = id && (this.autoSelectBranch || !this.children(id).length) ? id : null;
    this.rebuild();
    this.change(this.value);
    this.touch();
  }
  useBranch(): void {
    if (this.disabled) return;
    this.value = this.branch?.id ?? null;
    this.change(this.value);
    this.touch();
  }
  clear(): void {
    this.path = [];
    this.value = null;
    this.rebuild();
    this.change(null);
    this.touch();
  }
  get branch(): Row | undefined {
    return this.locations.find((row) => row.id === this.path.filter(Boolean).slice(-1)[0]);
  }
  get selectedPath(): string {
    return this.value === null ? '' : locationPath(this.value, this.locations);
  }
  private rebuild(): void {
    const ids = new Set(this.locations.map((row) => row.id));
    this.steps = [
      {
        label: 'Sede',
        options: this.locations.filter((row) => row.parentId == null || !ids.has(row.parentId)),
        value: this.path[0] ?? null,
      },
    ];
    const root = this.path[0];
    if (!root) return;
    const children = this.children(root);
    if (!children.length) return;
    const branches = children.filter((row) => this.children(row.id).length);
    const direct = children.filter((row) => !this.children(row.id).length);
    this.steps.push({
      label: 'Piso',
      options: [...branches, ...(direct.length ? [{ id: 0, name: 'Sin piso asignado' }] : [])],
      value: this.path[1] ?? null,
    });
    if (this.path[1] === undefined) return;
    let options = this.path[1] === 0 ? direct : this.children(this.path[1]);
    const seen = new Set(this.path.slice(0, 2));
    for (let index = 2; options.length && index < this.locations.length + 2; index++) {
      options = options.filter((row) => !seen.has(row.id));
      if (!options.length) break;
      const id = this.path[index];
      this.steps.push({
        label: index === 2 ? 'Ambiente / espacio' : 'Subambiente',
        options,
        value: id ?? null,
      });
      if (id === undefined || seen.has(id)) break;
      seen.add(id);
      options = this.children(id);
    }
  }
}
