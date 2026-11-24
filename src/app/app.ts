import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AGENCY_CONTACT } from './core/data/navigation';
import { Footer } from './layout/footer/footer';
import { Header } from './layout/header/header';
import { Icon } from './shared/icon/icon';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  templateUrl: './app.html',
})
export class App {
  protected readonly contact = AGENCY_CONTACT;
}
