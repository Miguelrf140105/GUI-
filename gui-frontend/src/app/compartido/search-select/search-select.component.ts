import { Component, Input } from '@angular/core';
import { forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  NG_VALIDATORS,
  Validator,
  ValidationErrors,
} from '@angular/forms';
@Component({
  selector: 'gui-search-select',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchSelectComponent),
      multi: true,
    },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => SearchSelectComponent), multi: true },
  ],
  templateUrl: './search-select.component.html',
  styles: [':host { display: contents; }'],
})
export class SearchSelectComponent implements ControlValueAccessor, Validator {
  @Input() options: any[] = [];
  @Input() label = 'Buscar';
  @Input() required = false;
  value: any = null;
  disabled = false;
  change: (_: any) => void = () => {};
  touch: () => void = () => {};
  writeValue(value: any): void {
    this.value = value === '' ? null : value;
  }
  registerOnChange(fn: any): void {
    this.change = fn;
  }
  registerOnTouched(fn: any): void {
    this.touch = fn;
  }
  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }
  select(value: any): void {
    this.value = value;
    this.change(value);
    this.touch();
  }
  validate(): ValidationErrors | null {
    return this.required && (this.value === null || this.value === '') ? { required: true } : null;
  }
}
