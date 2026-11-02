import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Row } from '../../models/shared.model';

@Component({
  selector: 'gui-location-tree',
  templateUrl: './location-tree.component.html',
  styleUrls: ['./location-tree.component.scss'],
})
export class LocationTreeComponent {
  @Input() locations: Row[] = [];
  @Input() editable = false;
  @Input() creatable = false;
  @Input() deletable = false;
  @Output() remove = new EventEmitter<Row>();
  @Output() createChild = new EventEmitter<Row>();
  @Output() edit = new EventEmitter<Row>();
  private expanded = new Map<number, boolean>();

  // Un usuario con alcance parcial puede recibir una rama sin sus antecesores.
  children(parent: number | null, seen: number[]): Row[] {
    const ids = new Set(this.locations.map((row) => row.id));
    return this.locations.filter(
      (row) =>
        (parent === null
          ? row.parentId == null || !ids.has(row.parentId)
          : row.parentId === parent) && !seen.includes(row.id)
    );
  }
  hasChildren(id: number): boolean {
    return this.locations.some((row) => row.parentId === id);
  }
  isExpanded(row: Row): boolean {
    return this.expanded.get(row.id) ?? row.parentId == null;
  }
  toggle(row: Row): void {
    this.expanded.set(row.id, !this.isExpanded(row));
  }
  next(seen: number[], id: number): number[] {
    return [...seen, id];
  }
}
